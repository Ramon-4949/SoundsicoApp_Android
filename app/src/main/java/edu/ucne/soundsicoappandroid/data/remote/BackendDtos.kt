package edu.ucne.soundsicoappandroid.data.remote

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MilestoneCheckInDto(
    val id: String,
    @SerialName("hito_id") val milestoneId: String,
    @SerialName("usuario_id") val userId: String,
    @SerialName("created_at") val createdAt: String,
    val evaluacion: String
) {
    fun toDomain() = MilestoneCheckIn(id, milestoneId, userId, createdAt, evaluacion)
}

@Serializable
data class AssignmentNoteDto(
    val id: String,
    @SerialName("usuario_id") val userId: String? = null,
    val contenido: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("autor_nombre") val authorName: String? = null
) {
    fun toDomain() = AssignmentNote(id, userId, contenido, createdAt, authorName)
}

@Serializable
data class AssignmentCollaboratorDto(
    val id: String,
    val nombre: String,
    val cargo: String? = null,
    @SerialName("es_supervisor") val supervisor: Boolean
) {
    fun toDomain() = AssignmentCollaborator(id, nombre, cargo, supervisor)
}

@Serializable
data class FullManagedAccountDto(
    val id: String,
    val nombre: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val cargo: String? = null,
    val estado: String,
    val fecha: String
) {
    fun toDomain() = ManagedAccount(id, nombre, email, telefono, cargo,
        AccountAccessState.entries.firstOrNull { it.value == estado } ?: AccountAccessState.Pending, fecha)
}

@Serializable
data class PerformanceWeekDto(val semana: Int, val cantidad: Int) {
    fun toDomain() = PerformanceWeek(semana, cantidad)
}

@Serializable
data class EmployeePerformanceDto(
    val id: String,
    val nombre: String,
    val cargo: String,
    val temprano: Int,
    @SerialName("a_tiempo") val onTime: Int,
    val tardio: Int,
    @SerialName("sin_confirmar") val unconfirmed: Int,
    @SerialName("retraso_medio_minutos") val averageDelay: Double? = null,
    @SerialName("notas_semanales") val weeklyNotes: List<PerformanceWeekDto> = emptyList(),
    val asignaciones: Int,
    val completadas: Int,
    val activas: Int,
    val vencidas: Int
) {
    fun toDomain() = EmployeePerformance(id, nombre, cargo, temprano, onTime, tardio, unconfirmed,
        averageDelay, weeklyNotes.map { it.toDomain() }, asignaciones, completadas, activas, vencidas)
}

@Serializable
data class EmployeeAvailabilityDto(
    val id: String,
    val nombre: String? = null,
    val cargo: String? = null,
    val rol: String? = null,
    val disponible: Boolean,
    @SerialName("ocupado_desde") val busyFrom: String? = null,
    @SerialName("ocupado_hasta") val busyUntil: String? = null
) {
    fun toDomain() = EmployeeAvailability(id, nombre, cargo, rol, disponible, busyFrom, busyUntil)
}

@Serializable
data class AdminAssignmentPageDto(
    val items: List<AssignmentDto>,
    @SerialName("has_more") val hasMore: Boolean,
    @SerialName("next_offset") val nextOffset: Int? = null
) {
    fun toDomain() = AdminAssignmentPage(items.map { it.toDomain("", true) }, hasMore, nextOffset)
}

@Serializable
data class EmployeeNotificationDto(
    val id: String,
    val titulo: String? = null,
    val mensaje: String? = null,
    val leida: Boolean? = null,
    @SerialName("fecha_creacion") val createdAt: String? = null,
    val tipo: String? = null,
    @SerialName("destino_id") val destinationId: String? = null,
    @SerialName("destino_tipo") val destinationType: String? = null,
    val estado: String? = null
) {
    fun toDomain() = EmployeeNotification(id, titulo, mensaje, leida == true, createdAt, tipo,
        destinationId, destinationType, estado)
}

@Serializable
data class BulletinCreateDto(
    val asunto: String,
    val mensaje: String,
    @SerialName("fecha_publicacion") val publishedAt: String,
    @SerialName("leido_por") val readBy: List<String> = emptyList()
)

@Serializable
data class BulletinUpdateDto(val asunto: String, val mensaje: String)

@Serializable
data class AssignmentMutationDto(
    val supervisores: List<String>,
    @SerialName("tipo_flujo") val flowType: String,
    val titulo: String,
    val ubicacion: String? = null,
    @SerialName("nivel_prioridad") val priority: String,
    val instrucciones: String? = null,
    @SerialName("fecha_creacion") val createdAt: String,
    @SerialName("fecha_limite") val deadline: String? = null
)

@Serializable
data class MilestoneMutationDto(
    val colaboradores: List<String>,
    val id: String,
    val orden: Int,
    @SerialName("descripcion") val title: String,
    @SerialName("hora_estimada") val estimatedTime: String,
    @SerialName("fecha_programada") val scheduledAt: String,
    @SerialName("estado_hito") val status: String,
    @SerialName("notas_incidencias") val incidentNotes: String? = null
)
