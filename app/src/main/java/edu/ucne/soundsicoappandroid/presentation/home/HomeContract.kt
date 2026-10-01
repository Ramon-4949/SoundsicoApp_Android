package edu.ucne.soundsicoappandroid.presentation.home

import edu.ucne.soundsicoappandroid.domain.model.*
import java.time.LocalDate

enum class HomeTab(val title: String) { Start("Inicio"), Messages("Mensajes"), Calendar("Calendario"), Profile("Perfil") }
enum class AssignmentFilter(val title: String) { All("Todas"), Pending("Pendientes"), Complete("Completadas"), Overdue("Vencidas") }

data class HomeState(
    val user: AuthUser,
    val profile: EmployeeProfile? = null,
    val tab: HomeTab = HomeTab.Start,
    val loading: Boolean = true,
    val signingOut: Boolean = false,
    val failure: String? = null,
    val content: HomeContent = HomeContent(emptyList(), emptyList()),
    val search: String = "",
    val filter: AssignmentFilter = AssignmentFilter.All,
    val selectedDate: LocalDate = LocalDate.now(),
    val assignment: Assignment? = null,
    val bulletin: Bulletin? = null
)

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object SignOut : HomeIntent
    data class SelectTab(val tab: HomeTab) : HomeIntent
    data class Search(val value: String) : HomeIntent
    data class Filter(val value: AssignmentFilter) : HomeIntent
    data class SelectDate(val value: LocalDate) : HomeIntent
    data class OpenAssignment(val value: Assignment) : HomeIntent
    data class OpenBulletin(val value: Bulletin) : HomeIntent
    data object CloseDetail : HomeIntent
}
