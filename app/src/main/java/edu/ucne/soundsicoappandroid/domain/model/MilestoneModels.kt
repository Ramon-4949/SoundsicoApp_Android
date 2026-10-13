package edu.ucne.soundsicoappandroid.domain.model

enum class MilestoneStatus(val value: String) {
    Completed("completado"),
    InProgress("en_curso"),
    Blocked("bloqueado")
}

data class Milestone(
    val id: String,
    val assignmentId: String? = null,
    val order: Int = 0,
    val title: String = "",
    val estimatedTime: String? = null,
    val scheduledAt: String? = null,
    val completed: Boolean = false,
    val status: String? = null,
    val incidentNotes: String? = null,
    val completedAt: String? = null,
    val slaOpen: Boolean? = null,
    val collaborators: List<MilestoneCollaborator> = emptyList()
) {
    val globallyCompleted: Boolean get() = if (collaborators.isEmpty()) completed else collaborators.all(MilestoneCollaborator::confirmed)
    fun isAssignedTo(userId: String): Boolean = collaborators.any { it.milestoneId == id && it.userId == userId }
    fun isConfirmedBy(userId: String): Boolean = collaborators.any { it.milestoneId == id && it.userId == userId && it.confirmed }
}

fun AssignmentDetails.confirmationBlock(milestoneId: String, userId: String): String? {
    val personal = assignment.milestones.filter { it.isAssignedTo(userId) }.sortedBy { it.order }
    val index = personal.indexOfFirst { it.id == milestoneId }
    if (index < 0) return "No estás asignado a este hito."
    fun Milestone.confirmed(): Boolean = isConfirmedBy(userId) ||
        checkIns.any { it.milestoneId == id && it.userId == userId }
    if (personal[index].confirmed()) return "Este hito ya fue confirmado."
    if (personal.take(index).any { !it.confirmed() }) return "Confirma primero tu hito anterior."
    return null
}

data class MilestoneDraft(
    val id: String,
    val collaboratorIds: List<String>,
    val order: Int,
    val title: String,
    val estimatedTime: String,
    val scheduledAt: String,
    val status: MilestoneStatus = MilestoneStatus.Blocked,
    val incidentNotes: String? = null
)

data class MilestoneCollaborator(
    val milestoneId: String,
    val userId: String,
    val status: String,
    val confirmedAt: String? = null,
    val scheduledAt: String? = null,
    val employee: Employee? = null
) {
    val confirmed: Boolean get() = confirmedAt != null
}

data class MilestoneCheckIn(
    val id: String,
    val milestoneId: String,
    val userId: String,
    val createdAt: String,
    val evaluation: String
)

data class AssignmentNote(
    val id: String,
    val userId: String?,
    val content: String,
    val createdAt: String,
    val authorName: String?
)

data class AssignmentCollaborator(
    val id: String,
    val name: String,
    val position: String?,
    val supervisor: Boolean
)
