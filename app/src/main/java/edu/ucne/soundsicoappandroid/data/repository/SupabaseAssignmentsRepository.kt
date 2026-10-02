package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.AssignmentDto
import edu.ucne.soundsicoappandroid.data.remote.AssignmentLinkDto
import edu.ucne.soundsicoappandroid.domain.model.Assignment
import edu.ucne.soundsicoappandroid.domain.repository.AssignmentsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class SupabaseAssignmentsRepository(private val client: SupabaseClient) : AssignmentsRepository {
    override suspend fun getAssignments(userId: String): List<Assignment> {
        val rows = mutableListOf<AssignmentDto>()
        var offset = 0L
        do {
            val page = client.from("asignacion_equipo").select(Columns.raw(LIST_COLUMNS)) {
                filter { eq("perfil_id", userId) }
                order("asignacion_id", Order.ASCENDING)
                range(offset, offset + PAGE_SIZE - 1)
            }.decodeList<AssignmentLinkDto>()
            rows += page.mapNotNull { it.asignaciones }
            offset += PAGE_SIZE
        } while (page.size == PAGE_SIZE.toInt())
        return rows.distinctBy { it.id }.map { it.toDomain(userId, false) }
            .sortedWith(compareBy<Assignment> { it.status == "completada" }.thenBy { it.deadline })
    }

    override suspend fun getAssignment(id: String, viewerId: String?): Assignment =
        client.from("asignaciones").select(Columns.raw(DETAIL_COLUMNS)) {
            filter { eq("id", id) }
        }.decodeSingle<AssignmentDto>().toDomain(viewerId.orEmpty(), viewerId == null)

    override fun observeAssignments(userId: String): Flow<List<Assignment>> = flow {
        val channel = client.channel("assignments-$userId-${System.nanoTime()}")
        val team = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "asignacion_equipo"
            filter("perfil_id", FilterOperator.EQ, userId)
        }
        val assignments = channel.postgresChangeFlow<PostgresAction>("public") { table = "asignaciones" }
        val milestones = channel.postgresChangeFlow<PostgresAction>("public") { table = "hitos_itinerario" }
        val checkIns = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "hitos_colaboradores"
            filter("usuario_id", FilterOperator.EQ, userId)
        }
        try {
            emit(getAssignments(userId))
            channel.subscribe(true)
            val polling = flow {
                while (true) {
                    delay(10_000)
                    emit(Unit)
                }
            }
            merge(team, assignments, milestones, checkIns).map { Unit }.mergeWith(polling).collect {
                emit(getAssignments(userId))
            }
        } finally {
            client.realtime.removeChannel(channel)
        }
    }

    private fun Flow<Unit>.mergeWith(other: Flow<Unit>): Flow<Unit> = merge(this, other)

    private companion object {
        const val PAGE_SIZE = 200L
        const val LIST_COLUMNS = "asignaciones(*,hitos_itinerario(*,hitos_colaboradores(*,perfiles(id,nombre_completo,rol,cargo))),asignacion_supervisores(usuario_id),asignacion_equipo(perfiles(id,nombre_completo,rol,cargo)))"
        const val DETAIL_COLUMNS = "*,hitos_itinerario(*,hitos_colaboradores(*,perfiles(id,nombre_completo,rol,cargo))),asignacion_supervisores(usuario_id),asignacion_equipo(perfiles(id,nombre_completo,rol,cargo))"
    }
}
