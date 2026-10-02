package edu.ucne.soundsicoappandroid.domain.model

data class Assignment(
    val id: String,
    val title: String,
    val type: String,
    val priority: String,
    val status: String,
    val location: String?,
    val deadline: String?,
    val instructions: String?,
    val scheduledDates: List<String> = emptyList(),
    val nextMilestoneTitle: String? = null,
    val nextMilestoneDate: String? = null,
    val createdAt: String? = null,
    val milestones: List<Milestone> = emptyList(),
    val team: List<Employee> = emptyList(),
    val supervisors: List<AssignmentSupervisor> = emptyList(),
    val viewingUserId: String? = null
)

data class Bulletin(
    val id: String,
    val subject: String,
    val message: String,
    val publishedAt: String? = null,
    val readBy: List<String> = emptyList()
)

data class BulletinDraft(val subject: String, val message: String)

data class HomeContent(
    val assignments: List<Assignment>,
    val bulletins: List<Bulletin>,
    val dashboard: AdminDashboard? = null,
    val nextOffset: Int? = null
)
