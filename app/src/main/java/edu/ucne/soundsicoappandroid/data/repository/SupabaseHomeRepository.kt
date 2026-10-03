package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.*
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.HomeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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

class SupabaseHomeRepository(private val client: SupabaseClient) : HomeRepository {
    override suspend fun load(userId: String, administrator: Boolean): HomeContent = coroutineScope {
        val bulletins = async { loadBulletins() }
        if (administrator) {
            val dashboard = async { loadDashboard() }
            val page = loadAdminAssignments(0, "todas", "")
            HomeContent(page.items, bulletins.await(), dashboard.await(), page.nextOffset)
        } else {
            HomeContent(loadEmployeeAssignments(userId), bulletins.await())
        }
    }

    override suspend fun loadEmployeeAssignments(userId: String): List<Assignment> {
        val result = mutableListOf<AssignmentDto>()
        var offset = 0L
        val columns = "asignaciones(*,hitos_itinerario(*,hitos_colaboradores(*,perfiles(id,nombre_completo,rol,cargo))),asignacion_supervisores(usuario_id),asignacion_equipo(perfiles(id,nombre_completo,rol,cargo)))"
        do {
            val page = client.from("asignacion_equipo").select(Columns.raw(columns)) {
                filter { eq("perfil_id", userId) }
                order("asignacion_id", Order.ASCENDING)
                range(offset, offset + 199)
            }.decodeList<AssignmentLinkDto>()
            result.addAll(page.mapNotNull { it.asignaciones })
            offset += 200
        } while (page.size == 200)
        return result.distinctBy { it.id }.map { it.toDomain(userId, false) }
            .sortedWith(compareBy<Assignment> { it.status == "completada" }.thenBy { it.deadline })
    }

    override suspend fun loadDashboard(): AdminDashboard = coroutineScope {
        val metrics = async { client.postgrest.rpc("admin_dashboard_summary").decodeAs<DashboardMetricsDto>().toDomain() }
        val accounts = async {
            val pending = mutableListOf<PendingAccount>()
            var offset = 0
            do {
                val page = client.postgrest.rpc("admin_list_accounts", buildJsonObject { put("p_offset", offset) })
                    .decodeList<ManagedAccountDto>()
                pending.addAll(page.filter { it.estado == "pendiente" }.map { it.toDomain() })
                offset += page.size
            } while (page.size == 200)
            pending.distinctBy { it.id }
        }
        AdminDashboard(metrics.await(), accounts.await())
    }

    override suspend fun loadAdminAssignments(offset: Int, filter: String, search: String): AssignmentPage =
        client.postgrest.rpc("admin_assignments_page", buildJsonObject {
            put("p_offset", offset)
            put("p_limit", 50)
            put("p_estado", filter)
            put("p_busqueda", search)
        }).decodeAs<AssignmentPageDto>().toDomain()

    override suspend fun reviewAccount(userId: String, approved: Boolean) {
        client.postgrest.rpc("admin_review_account", buildJsonObject {
            put("p_user_id", userId)
            put("p_estado", if (approved) "aprobada" else "rechazada")
        })
    }

    override fun observe(userId: String, administrator: Boolean): Flow<HomeContent> = flow {
        val channel = client.channel("home-$userId-${System.nanoTime()}")
        val assignments = channel.postgresChangeFlow<PostgresAction>("public") { table = "asignaciones" }
        val milestones = channel.postgresChangeFlow<PostgresAction>("public") { table = "hitos_itinerario" }
        val collaborators = channel.postgresChangeFlow<PostgresAction>("public") { table = "hitos_colaboradores" }
        val bulletins = channel.postgresChangeFlow<PostgresAction>("public") { table = "comunicados" }
        val profiles = channel.postgresChangeFlow<PostgresAction>("public") { table = "perfiles" }
        val team = channel.postgresChangeFlow<PostgresAction>("public") { table = "asignacion_equipo" }
        val notifications = channel.postgresChangeFlow<PostgresAction>("public") { table = "notificaciones_app" }
        try {
            emit(load(userId, administrator))
            channel.subscribe(true)
            val polling = flow {
                while (true) {
                    delay(10_000)
                    emit(Unit)
                }
            }
            val changes = if (administrator) merge(assignments, milestones, collaborators, bulletins, profiles, notifications)
            else merge(assignments, milestones, collaborators, bulletins, team)
            changes.map { Unit }.mergeWith(polling).collect { emit(load(userId, administrator)) }
        } finally {
            client.realtime.removeChannel(channel)
        }
    }

    private suspend fun loadBulletins(): List<Bulletin> {
        val result = mutableListOf<BulletinDto>()
        var offset = 0L
        do {
            val page = client.from("comunicados").select {
                order("id", Order.DESCENDING)
                range(offset, offset + 199)
            }.decodeList<BulletinDto>()
            result.addAll(page)
            offset += 200
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    private fun Flow<Unit>.mergeWith(other: Flow<Unit>): Flow<Unit> = merge(this, other)
}
