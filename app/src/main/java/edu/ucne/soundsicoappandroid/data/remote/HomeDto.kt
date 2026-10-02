package edu.ucne.soundsicoappandroid.data.remote

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

@Serializable
data class AssignmentDto(
    val id: String,
    @SerialName("titulo") val title: String,
    @SerialName("tipo_flujo") val type: String? = null,
    @SerialName("nivel_prioridad") val priority: String? = null,
    @SerialName("estado") val status: String? = null,
    @SerialName("ubicacion") val location: String? = null,
    @SerialName("fecha_limite") val deadline: String? = null,
    @SerialName("instrucciones") val instructions: String? = null,
    @SerialName("fecha_creacion") val createdAt: String? = null,
    @SerialName("hitos_itinerario") val milestones: List<MilestoneDto> = emptyList(),
    @SerialName("asignacion_supervisores") val supervisors: List<SupervisorDto> = emptyList(),
    @SerialName("asignacion_equipo") val team: List<AssignmentTeamDto> = emptyList()
) {
    fun toDomain(userId: String, administrator: Boolean, now: Instant = Instant.now()): Assignment {
        val ordered = milestones.sortedBy { it.order ?: 0 }
        val personal = ordered.takeIf { !administrator && supervisors.none { it.userId == userId } && ordered.isNotEmpty() }
            ?.filter { milestone -> milestone.collaborators.any { it.userId == userId } }
        val personalDeadline = personal?.lastOrNull()?.date
        val displayDeadline = if (personal != null) personalDeadline else deadline ?: ordered.lastOrNull()?.date
        val complete = personal?.let { own -> own.isNotEmpty() && own.all { milestone ->
            milestone.collaborators.any { it.userId == userId && it.confirmedAt != null }
        } }
        val normalizedStatus = status.orEmpty().lowercase()
        val completedByStatus = normalizedStatus in COMPLETED_STATUSES
        val overdue = !completedByStatus && displayDeadline.toInstantOrNull()?.let { !it.isAfter(now) } == true
        val displayStatus = when {
            complete == true || completedByStatus -> "completada"
            overdue || normalizedStatus == "vencida" -> "vencida"
            personal == null -> normalizedStatus
            personal.any { milestone -> milestone.collaborators.any { it.userId == userId && it.confirmedAt != null } } -> "en_curso"
            else -> "pendiente"
        }
        val visible = personal ?: ordered
        val next = visible.firstOrNull { milestone ->
            if (personal != null) milestone.collaborators.none { it.userId == userId && it.confirmedAt != null }
            else milestone.completed != true
        } ?: visible.lastOrNull()
        return Assignment(id, title, type.orEmpty(), priority.orEmpty(), displayStatus, location,
            displayDeadline, instructions, visible.mapNotNull { it.date }, next?.description, next?.date,
            createdAt, ordered.map { it.toDomain() }, team.mapNotNull { it.perfiles?.toDomain() },
            supervisors.map { AssignmentSupervisor(it.userId) }, userId.takeIf { it.isNotBlank() })
    }

    private fun String?.toInstantOrNull(): Instant? {
        if (this == null) return null
        return runCatching { OffsetDateTime.parse(this).toInstant() }
            .recoverCatching { Instant.parse(this) }
            .recoverCatching { LocalDateTime.parse(this).atZone(DOMINICAN_ZONE).toInstant() }
            .getOrNull()
    }

    private companion object {
        val COMPLETED_STATUSES = setOf("completada", "completado", "finalizada", "finalizado")
        val DOMINICAN_ZONE: ZoneId = ZoneId.of("America/Santo_Domingo")
    }
}

@Serializable
data class MilestoneDto(
    val id: String = "",
    @SerialName("asignacion_id") val assignmentId: String? = null,
    @SerialName("descripcion") val description: String? = null,
    @SerialName("completado") val completed: Boolean? = null,
    @SerialName("orden") val order: Int? = null,
    @SerialName("fecha_programada") val date: String? = null,
    @SerialName("hora_estimada") val estimatedTime: String? = null,
    @SerialName("estado_hito") val status: String? = null,
    @SerialName("notas_incidencias") val incidentNotes: String? = null,
    @SerialName("hora_real_completado") val completedAt: String? = null,
    @SerialName("sla_abierto") val slaOpen: Boolean? = null,
    @SerialName("hitos_colaboradores") val collaborators: List<CollaboratorDto> = emptyList()
) {
    fun toDomain() = Milestone(id, assignmentId, order ?: 0, description.orEmpty(), estimatedTime, date,
        completed == true || status == "completado", status, incidentNotes, completedAt, slaOpen,
        collaborators.map { it.toDomain() })
}

@Serializable
data class CollaboratorDto(
    @SerialName("hito_id") val milestoneId: String = "",
    @SerialName("usuario_id") val userId: String,
    val estado: String = "",
    @SerialName("confirmado_at") val confirmedAt: String? = null,
    @SerialName("hora_programada") val scheduledAt: String? = null,
    val perfiles: EmployeeDto? = null
) {
    fun toDomain() = MilestoneCollaborator(milestoneId, userId, estado,
        confirmedAt, scheduledAt, perfiles?.toDomain())
}

@Serializable
data class SupervisorDto(@SerialName("usuario_id") val userId: String)

@Serializable
data class EmployeeDto(
    val id: String,
    @SerialName("nombre_completo") val name: String? = null,
    val rol: String? = null,
    val cargo: String? = null
) {
    fun toDomain() = Employee(id, name.orEmpty(), rol.orEmpty(), cargo)
}

@Serializable
data class AssignmentTeamDto(val perfiles: EmployeeDto? = null)

@Serializable
data class AssignmentLinkDto(val asignaciones: AssignmentDto? = null)

@Serializable
data class BulletinDto(
    val id: String,
    val asunto: String,
    val mensaje: String,
    @SerialName("fecha_publicacion") val publishedAt: String? = null,
    @SerialName("leido_por") val readBy: List<String> = emptyList()
) {
    fun toDomain() = Bulletin(id, asunto, mensaje, publishedAt, readBy)
}
