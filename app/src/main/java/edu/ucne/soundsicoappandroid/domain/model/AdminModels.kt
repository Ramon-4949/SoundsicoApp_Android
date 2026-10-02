package edu.ucne.soundsicoappandroid.domain.model

enum class AccountAccessState(val value: String) {
    Pending("pendiente"),
    Approved("aprobada"),
    Rejected("rechazada")
}

data class ManagedAccount(
    val id: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    val position: String?,
    val state: AccountAccessState,
    val date: String
)

data class PerformanceWeek(val week: Int, val count: Int)

data class EmployeePerformance(
    val id: String,
    val name: String,
    val position: String,
    val early: Int,
    val onTime: Int,
    val late: Int,
    val unconfirmed: Int,
    val averageDelayMinutes: Double?,
    val weeklyNotes: List<PerformanceWeek>,
    val assignments: Int,
    val completed: Int,
    val active: Int,
    val overdue: Int
)

data class EmployeeAvailability(
    val id: String,
    val name: String?,
    val position: String?,
    val role: String?,
    val available: Boolean,
    val busyFrom: String?,
    val busyUntil: String?
)

data class AssignmentBookingWindow(val start: String, val end: String)

data class AdminAssignmentPage(
    val items: List<Assignment>,
    val hasMore: Boolean,
    val nextOffset: Int?
)
