package edu.ucne.soundsicoappandroid.presentation.admindashboard

import edu.ucne.soundsicoappandroid.domain.model.AdminDashboard
import edu.ucne.soundsicoappandroid.domain.model.EmployeePerformance
import edu.ucne.soundsicoappandroid.domain.model.PendingAccount

data class AdminDashboardState(
    val dashboard: AdminDashboard? = null,
    val performance: List<EmployeePerformance> = emptyList(),
    val loading: Boolean = true,
    val reviewingAccountId: String? = null,
    val failure: String? = null
)

sealed interface AdminDashboardIntent {
    data object Refresh : AdminDashboardIntent
    data class ReviewAccount(val account: PendingAccount, val approved: Boolean) : AdminDashboardIntent
    data object DismissError : AdminDashboardIntent
}
