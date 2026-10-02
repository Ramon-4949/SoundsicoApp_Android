package edu.ucne.soundsicoappandroid.domain.model

data class DashboardMetrics(
    val total: Int,
    val assigned: Int,
    val active: Int,
    val deliveryRate: Double,
    val createdLastSevenDays: List<Int>
)

data class PendingAccount(val id: String, val name: String, val email: String, val position: String)

data class AssignmentPage(val items: List<Assignment>, val nextOffset: Int?)

data class AdminDashboard(val metrics: DashboardMetrics, val pendingAccounts: List<PendingAccount>)
