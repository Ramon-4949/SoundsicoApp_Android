package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.*
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.HomeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseHomeRepository(private val client: SupabaseClient) : HomeRepository {
    override suspend fun load(userId: String, administrator: Boolean): HomeContent = coroutineScope {
        val bulletins = async { loadBulletins() }
        val dashboard = if (administrator) loadDashboard() else null
        HomeContent(bulletins.await(), dashboard)
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

    override suspend fun loadAssignmentPage(
        userId: String, administrator: Boolean, offset: Int, filter: AssignmentFeedFilter,
        search: String, day: AssignmentDay?
    ): AssignmentPage {
        val adminHome = administrator && day == null
        val params = buildJsonObject {
            put("p_offset", offset)
            put("p_limit", ASSIGNMENT_PAGE_SIZE)
            if (adminHome) {
                put("p_estado", filter.adminValue)
                put("p_busqueda", search)
            } else {
                put("p_status", filter.feedValue)
                put("p_search", search)
                if (day != null) {
                    put("p_day_start", day.start.toString())
                    put("p_day_end", day.end.toString())
                }
            }
        }
        val function = if (adminHome) "admin_assignments_page" else "assignments_feed_page"
        return client.postgrest.rpc(function, params).decodeAs<AssignmentPageDto>().toDomain(userId, administrator, offset)
    }

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
            channel.subscribe(true)
            val polling = flow {
                while (true) {
                    delay(10_000)
                    emit(Unit)
                }
            }
            val changes = if (administrator) merge(assignments, milestones, collaborators, bulletins, profiles, notifications)
            else merge(assignments, milestones, collaborators, bulletins, team)
            merge(changes.map { Unit }, polling).conflate().collect { emit(load(userId, administrator)) }
        } finally {
            client.realtime.removeChannel(channel)
        }
    }

    private suspend fun loadBulletins(): List<Bulletin> = SupabaseBulletinsRepository(client).getBulletins()

}
