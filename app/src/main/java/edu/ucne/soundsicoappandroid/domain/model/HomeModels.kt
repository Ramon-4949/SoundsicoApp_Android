package edu.ucne.soundsicoappandroid.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val ASSIGNMENT_PAGE_SIZE = 10

enum class AssignmentFeedFilter(val adminValue: String, val feedValue: String) {
    Pending("pendientes", "pending"),
    Overdue("vencidas", "overdue"),
    Completed("completadas", "completed"),
    All("todas", "all")
}

data class AssignmentDay(val start: Instant, val end: Instant) {
    companion object {
        fun from(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()) =
            AssignmentDay(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
    }
}

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
    val bulletins: List<Bulletin> = emptyList(),
    val dashboard: AdminDashboard? = null
)
