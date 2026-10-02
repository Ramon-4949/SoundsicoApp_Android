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
)

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
