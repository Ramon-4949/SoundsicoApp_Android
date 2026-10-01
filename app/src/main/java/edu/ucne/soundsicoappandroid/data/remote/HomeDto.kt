package edu.ucne.soundsicoappandroid.data.remote

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("hitos_itinerario") val milestones: List<MilestoneDto> = emptyList(),
    @SerialName("asignacion_supervisores") val supervisors: List<SupervisorDto> = emptyList()
) {
    fun toDomain(userId: String, administrator: Boolean): Assignment {
        val ordered = milestones.sortedBy { it.order ?: 0 }
        val personal = ordered.takeIf { !administrator && supervisors.none { it.userId == userId } && ordered.isNotEmpty() }
            ?.filter { milestone -> milestone.collaborators.any { it.userId == userId } }
        val personalDeadline = personal?.lastOrNull()?.date
        val displayDeadline = if (personal != null) personalDeadline else deadline ?: ordered.lastOrNull()?.date
        val complete = personal?.let { own -> own.isNotEmpty() && own.all { milestone ->
            milestone.collaborators.any { it.userId == userId && it.confirmed }
        } }
        val displayStatus = when {
            complete == true -> "completada"
            personal == null -> status.orEmpty()
            personalDeadline != null && runCatching { java.time.OffsetDateTime.parse(personalDeadline).toInstant().isBefore(java.time.Instant.now()) }.getOrDefault(false) -> "vencida"
            personal.any { milestone -> milestone.collaborators.any { it.userId == userId && it.confirmed } } -> "en_curso"
            else -> "pendiente"
        }
        return Assignment(id, title, type.orEmpty(), priority.orEmpty(), displayStatus, location,
            displayDeadline, instructions, ordered.mapNotNull { it.date })
    }
}

@Serializable
data class MilestoneDto(
    @SerialName("orden") val order: Int? = null,
    @SerialName("fecha_programada") val date: String? = null,
    @SerialName("hitos_colaboradores") val collaborators: List<CollaboratorDto> = emptyList()
)

@Serializable
data class CollaboratorDto(@SerialName("usuario_id") val userId: String, @SerialName("confirmado") val confirmed: Boolean = false)

@Serializable
data class SupervisorDto(@SerialName("usuario_id") val userId: String)

@Serializable
data class AssignmentLinkDto(val asignaciones: AssignmentDto? = null)

@Serializable
data class BulletinDto(val id: String, val asunto: String, val mensaje: String) {
    fun toDomain() = Bulletin(id, asunto, mensaje)
}
