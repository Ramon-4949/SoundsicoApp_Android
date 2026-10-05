package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.*
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.MilestonesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseMilestonesRepository(private val client: SupabaseClient) : MilestonesRepository {
    override suspend fun getDetails(assignmentId: String, userId: String, administrator: Boolean): AssignmentDetails = coroutineScope {
        val assignment = async { SupabaseAssignmentsRepository(client).getAssignment(assignmentId, userId.takeUnless { administrator }) }
        val checkIns = async { getCheckIns(assignmentId, userId) }
        val notes = async { getNotes(assignmentId) }
        val collaborators = async { getCollaborators(assignmentId) }
        AssignmentDetails(assignment.await(), checkIns.await(), notes.await(), collaborators.await())
    }

    override suspend fun getCheckIns(assignmentId: String, userId: String): List<MilestoneCheckIn> {
        val result = mutableListOf<MilestoneCheckInDto>()
        var offset = 0L
        do {
            val page = client.from("confirmaciones_hitos").select {
                filter {
                    eq("asignacion_id", assignmentId)
                    eq("usuario_id", userId)
                }
                order("id", Order.ASCENDING)
                range(offset, offset + 199)
            }.decodeList<MilestoneCheckInDto>()
            result += page
            offset += 200
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    override suspend fun getNotes(assignmentId: String): List<AssignmentNote> {
        val result = mutableListOf<AssignmentNoteDto>()
        var offset = 0
        do {
            val page = client.postgrest.rpc("assignment_notes", buildJsonObject {
                put("p_assignment", assignmentId)
                put("p_offset", offset)
                put("p_limit", 200)
            }).decodeList<AssignmentNoteDto>()
            result += page
            offset += 200
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    override suspend fun getCollaborators(assignmentId: String): List<AssignmentCollaborator> =
        client.postgrest.rpc("assignment_collaborators", buildJsonObject {
            put("p_assignment", assignmentId)
        }).decodeList<AssignmentCollaboratorDto>().map { it.toDomain() }

    override suspend fun checkIn(milestoneId: String, userId: String): MilestoneCheckIn {
        client.postgrest.rpc("check_in_milestone", buildJsonObject { put("p_hito_id", milestoneId) })
        return client.from("confirmaciones_hitos").select {
            filter {
                eq("hito_id", milestoneId)
                eq("usuario_id", userId)
            }
        }.decodeSingle<MilestoneCheckInDto>().toDomain()
    }

    override suspend fun undoCheckIn(milestoneId: String) {
        client.postgrest.rpc("undo_milestone_confirmation", buildJsonObject {
            put("p_hito_id", milestoneId)
        })
    }

    override suspend fun addNote(id: String, assignmentId: String, content: String) {
        client.postgrest.rpc("add_assignment_note", buildJsonObject {
            put("p_id", id)
            put("p_asignacion_id", assignmentId)
            put("p_contenido", content.trim())
        })
    }

    override fun observeDetails(assignmentId: String, userId: String, administrator: Boolean): Flow<AssignmentDetails> = flow {
        val channel = client.channel("milestone-detail-$assignmentId-${System.nanoTime()}")
        val checkIns = channel.postgresChangeFlow<PostgresAction>("public") { table = "hitos_colaboradores" }
        val milestones = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "hitos_itinerario"
            filter("asignacion_id", FilterOperator.EQ, assignmentId)
        }
        try {
            emit(getDetails(assignmentId, userId, administrator))
            channel.subscribe(true)
            val polling = flow {
                while (true) {
                    delay(10_000)
                    emit(Unit)
                }
            }
            merge(checkIns, milestones).map { Unit }.mergeWith(polling).collect {
                emit(getDetails(assignmentId, userId, administrator))
            }
        } finally {
            client.realtime.removeChannel(channel)
        }
    }

    private fun Flow<Unit>.mergeWith(other: Flow<Unit>): Flow<Unit> = merge(this, other)
}
