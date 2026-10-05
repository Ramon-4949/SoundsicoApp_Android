package edu.ucne.soundsicoappandroid.presentation.home

import edu.ucne.soundsicoappandroid.domain.model.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

enum class HomeTab(val title: String) { Start("Inicio"), Messages("Mensajes"), Calendar("Calendario"), Profile("Perfil") }
enum class AssignmentFilter(val title: String, val serverValue: String) {
    All("Todas", "todas"), Pending("Pendientes", "pendientes"),
    Overdue("Vencidas", "vencidas"), Complete("Completadas", "completadas")
}
enum class HomeAudience { Resolving, Restricted, Administrator, Employee }
enum class ProfilePage { Overview, Performance, Accounts }
data class AccountReview(val account: PendingAccount, val approved: Boolean)

data class HomeState(
    val user: AuthUser,
    val profile: EmployeeProfile? = null,
    val tab: HomeTab = HomeTab.Start,
    val notificationsOpen: Boolean = false,
    val unreadNotifications: Int = 0,
    val unreadMessages: Int = 0,
    val unreadBulletinIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val signingOut: Boolean = false,
    val failure: String? = null,
    val content: HomeContent = HomeContent(emptyList(), emptyList()),
    val search: String = "",
    val searching: Boolean = false,
    val filter: AssignmentFilter = AssignmentFilter.All,
    val selectedDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val todayOnly: Boolean = false,
    val assignment: Assignment? = null,
    val bulletin: Bulletin? = null,
    val loadingMore: Boolean = false,
    val listLoading: Boolean = false,
    val listFailure: String? = null,
    val approvalsOpen: Boolean = false,
    val review: AccountReview? = null,
    val savingAccount: Boolean = false,
    val actionFailure: String? = null,
    val calendarAssignments: List<Assignment>? = null,
    val calendarLoading: Boolean = false,
    val calendarFailure: String? = null,
    val profilePage: ProfilePage = ProfilePage.Overview,
    val accounts: List<ManagedAccount> = emptyList(),
    val accountsLoading: Boolean = false,
    val accountsFailure: String? = null,
    val reviewingAccount: String? = null,
    val performanceMonth: java.time.YearMonth = java.time.YearMonth.now(java.time.ZoneId.of("America/Santo_Domingo")),
    val performance: List<EmployeePerformance> = emptyList(),
    val performanceLoading: Boolean = false,
    val performanceFailure: String? = null,
    val biometricEnabled: Boolean = false,
    val deleteAccountConfirmation: Boolean = false,
    val deletingAccount: Boolean = false
) {
    val audience: HomeAudience get() = when {
        profile == null -> HomeAudience.Resolving
        profile.access != AccountAccess.Approved -> HomeAudience.Restricted
        profile.isAdministrator -> HomeAudience.Administrator
        else -> HomeAudience.Employee
    }

    val visibleAssignments: List<Assignment> get() {
        if (audience == HomeAudience.Administrator) return content.assignments
        return content.assignments.filter { assignment ->
            (!todayOnly || assignment.occursOn(today)) &&
                (assignment.title + " " + assignment.location.orEmpty()).contains(search.trim(), true) &&
                when (filter) {
                    AssignmentFilter.All -> true
                    AssignmentFilter.Pending -> assignment.status in listOf("pendiente", "en_curso")
                    AssignmentFilter.Complete -> assignment.status == "completada"
                    AssignmentFilter.Overdue -> assignment.status == "vencida"
                }
        }
    }
}

fun Assignment.occursOn(date: LocalDate): Boolean = (scheduledDates + listOfNotNull(deadline)).any {
    runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() == date }.getOrDefault(false)
}

sealed interface HomeIntent {
    data object OpenNotifications : HomeIntent
    data object CloseNotifications : HomeIntent
    data object Refresh : HomeIntent
    data object SignOut : HomeIntent
    data object ToggleSearch : HomeIntent
    data object ShowAll : HomeIntent
    data object ShowToday : HomeIntent
    data object LoadMore : HomeIntent
    data object RetryAssignments : HomeIntent
    data object OpenApprovals : HomeIntent
    data object CloseApprovals : HomeIntent
    data class RequestReview(val account: PendingAccount, val approved: Boolean) : HomeIntent
    data object ConfirmReview : HomeIntent
    data object CancelReview : HomeIntent
    data object DismissActionError : HomeIntent
    data class SelectTab(val tab: HomeTab) : HomeIntent
    data class Search(val value: String) : HomeIntent
    data class Filter(val value: AssignmentFilter) : HomeIntent
    data class SelectDate(val value: LocalDate) : HomeIntent
    data class OpenAssignment(val value: Assignment) : HomeIntent
    data class OpenBulletin(val value: Bulletin) : HomeIntent
    data object RetryAccounts : HomeIntent
    data class ReviewManagedAccount(val id: String, val access: AccountAccessState) : HomeIntent
    data class ChangePerformanceMonth(val month: java.time.YearMonth) : HomeIntent
    data object OpenDashboard : HomeIntent
    data object OpenPerformance : HomeIntent
    data object ClosePerformance : HomeIntent
    data object RetryPerformance : HomeIntent
    data class SetBiometric(val enabled: Boolean) : HomeIntent
    data object RequestDeleteAccount : HomeIntent
    data object CancelDeleteAccount : HomeIntent
    data object ConfirmDeleteAccount : HomeIntent
    data object CloseDetail : HomeIntent
}
