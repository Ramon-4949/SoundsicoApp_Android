package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.*
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.AdminRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAdminRepository(private val client: SupabaseClient) : AdminRepository {
    override suspend fun getDashboardMetrics(): DashboardMetrics =
        client.postgrest.rpc("admin_dashboard_summary").decodeAs<DashboardMetricsDto>().toDomain()

    override suspend fun getAssignments(
        offset: Int,
        limit: Int,
        filter: AdminAssignmentFilter,
        search: String
    ): AdminAssignmentPage = client.postgrest.rpc("admin_assignments_page", buildJsonObject {
        put("p_offset", offset)
        put("p_limit", limit)
        put("p_estado", filter.value)
        put("p_busqueda", search.trim())
    }).decodeAs<AdminAssignmentPageDto>().toDomain()

    override suspend fun getEmployees(): List<Employee> {
        val result = mutableListOf<EmployeeDto>()
        var offset = 0L
        do {
            val page = client.from("perfiles").select(Columns.raw("id,nombre_completo,rol,cargo")) {
                order("nombre_completo", Order.ASCENDING)
                range(offset, offset + 199)
            }.decodeList<EmployeeDto>()
            result += page
            offset += 200
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    override suspend fun createAssignment(draft: AssignmentDraft): Assignment {
        validate(draft)
        val id = client.postgrest.rpc("admin_create_assignment", mutationParams(draft)).decodeAs<String>()
        return fetchAssignment(id)
    }

    override suspend fun updateAssignment(id: String, draft: AssignmentDraft): Assignment {
        validate(draft)
        client.postgrest.rpc("admin_update_assignment", buildJsonObject {
            put("p_asignacion_id", id)
            mutationParams(draft).forEach { (key, value) -> put(key, value) }
        })
        return fetchAssignment(id)
    }

    override suspend fun deleteAssignment(id: String) {
        client.postgrest.rpc("admin_delete_assignment", buildJsonObject { put("p_asignacion_id", id) })
    }

    override suspend fun getAccounts(): List<ManagedAccount> {
        val result = mutableListOf<FullManagedAccountDto>()
        var offset = 0
        do {
            val page = client.postgrest.rpc("admin_list_accounts", buildJsonObject {
                put("p_offset", offset)
            }).decodeList<FullManagedAccountDto>()
            result += page
            offset += page.size
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    override suspend fun reviewAccount(userId: String, state: AccountAccessState) {
        client.postgrest.rpc("admin_review_account", buildJsonObject {
            put("p_user_id", userId)
            put("p_estado", state.value)
        })
    }

    override suspend fun getEmployeePerformance(month: String, employeeId: String?): List<EmployeePerformance> {
        val result = mutableListOf<EmployeePerformanceDto>()
        var offset = 0
        do {
            val page = client.postgrest.rpc("admin_employee_performance", buildJsonObject {
                put("p_month", month)
                put("p_offset", offset)
                employeeId?.let { put("p_employee", it) } ?: put("p_employee", JsonNull)
            }).decodeList<EmployeePerformanceDto>()
            result += page
            offset += page.size
        } while (page.size == 100)
        return result.map { it.toDomain() }
    }

    override suspend fun getEmployeeAvailability(
        window: AssignmentBookingWindow,
        excludingAssignmentId: String?
    ): List<EmployeeAvailability> {
        val result = mutableListOf<EmployeeAvailabilityDto>()
        var offset = 0
        do {
            val page = client.postgrest.rpc("admin_employee_availability", buildJsonObject {
                put("p_inicio", window.start)
                put("p_fin", window.end)
                excludingAssignmentId?.let { put("p_excluir", it) } ?: put("p_excluir", JsonNull)
                put("p_offset", offset)
            }).decodeList<EmployeeAvailabilityDto>()
            result += page
            offset += page.size
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    private suspend fun fetchAssignment(id: String): Assignment = client.from("asignaciones").select(
        Columns.raw("id,tipo_flujo,titulo,ubicacion,nivel_prioridad,instrucciones,estado,fecha_creacion,fecha_limite,hitos_itinerario(*,hitos_colaboradores(*,perfiles(id,nombre_completo,rol,cargo))),asignacion_supervisores(usuario_id),asignacion_equipo(perfiles(id,nombre_completo,rol,cargo))")
    ) { filter { eq("id", id) } }.decodeSingle<AssignmentDto>().toDomain("", true)

    private fun mutationParams(draft: AssignmentDraft) = buildJsonObject {
        val ordered = draft.milestones.sortedBy { it.order }
        val assignment = AssignmentMutationDto(
            draft.supervisorIds,
            draft.flowType.value,
            draft.title.trim(),
            draft.location?.trim(),
            draft.priority.value,
            draft.instructions?.trim(),
            draft.createdAt,
            ordered.lastOrNull()?.scheduledAt
        )
        val milestones = ordered.map {
            MilestoneMutationDto(it.collaboratorIds, it.id, it.order, it.title.trim(), it.estimatedTime,
                it.scheduledAt, it.status.value, it.incidentNotes)
        }
        put("p_asignacion", Json.encodeToJsonElement(AssignmentMutationDto.serializer(), assignment))
        put("p_empleados", Json.encodeToJsonElement(ListSerializer(String.serializer()), draft.employeeIds))
        put("p_hitos", Json.encodeToJsonElement(ListSerializer(MilestoneMutationDto.serializer()), milestones))
    }

    private fun validate(draft: AssignmentDraft) {
        require(draft.title.trim().length in 3..120)
        require(draft.employeeIds.isNotEmpty())
        require(draft.milestones.isNotEmpty())
        if (draft.flowType == AssignmentFlowType.Field) require(draft.location?.trim()?.length?.let { it in 3..180 } == true)
        if (draft.flowType == AssignmentFlowType.Administrative) require(draft.location == null)
        require(draft.milestones.map { it.order }.distinct().size == draft.milestones.size)
        require(draft.milestones.sortedBy { it.order }.zipWithNext().all { (a, b) -> a.scheduledAt <= b.scheduledAt })
        require(draft.milestones.all { it.collaboratorIds.isNotEmpty() && it.title.trim().length in 2..100 })
    }
}
