package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.*
import edu.ucne.soundsicoappandroid.domain.usecase.*
import edu.ucne.soundsicoappandroid.presentation.home.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = TestAuth()
    private val repository = TestHome()
    private val admin = TestAdmin()
    private val user = AuthUser("user", "ana@empresa.com", "Ana")

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private val preferences = TestPreferences()
    private fun model() = HomeViewModel(user, GetProfileUseCase(auth), GetHomeContentUseCase(repository), SignOutUseCase(auth),
        GetEmployeePerformanceUseCase(admin), DeleteAccountUseCase(auth), preferences, ManageAccountsUseCase(admin))

    @Test fun loadsAdminOnlyAfterApprovedServerProfile() = runTest(dispatcher) {
        auth.role = "admin"
        val model = model()
        assertEquals(HomeAudience.Resolving, model.state.value.audience)
        assertTrue(repository.loads.isEmpty())
        advanceUntilIdle()
        assertEquals(listOf(true), repository.loads)
        assertEquals(HomeAudience.Administrator, model.state.value.audience)
        assertEquals(17, model.state.value.content.dashboard?.metrics?.total)
        assertEquals(1, model.state.value.content.dashboard?.pendingAccounts?.size)
    }

    @Test fun employeesNeverRequestAdminPagesOrReviews() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.onIntent(HomeIntent.Filter(AssignmentFilter.Pending))
        model.onIntent(HomeIntent.LoadMore)
        model.onIntent(HomeIntent.OpenApprovals)
        model.onIntent(HomeIntent.RequestReview(repository.account, true))
        model.onIntent(HomeIntent.ConfirmReview)
        advanceUntilIdle()
        assertEquals(listOf(false), repository.loads)
        assertTrue(repository.requests.isEmpty())
        assertEquals(0, repository.reviews)
        assertFalse(model.state.value.approvalsOpen)
    }

    @Test fun revokedAccessClearsAdminDataAndOpenDetails() = runTest(dispatcher) {
        auth.role = "admin"
        val model = model()
        advanceUntilIdle()
        model.onIntent(HomeIntent.OpenApprovals)
        model.onIntent(HomeIntent.OpenAssignment(repository.assignment))
        auth.access = AccountAccess.Rejected
        model.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()
        assertEquals(HomeAudience.Restricted, model.state.value.audience)
        assertNull(model.state.value.content.dashboard)
        assertNull(model.state.value.assignment)
        assertFalse(model.state.value.approvalsOpen)
        assertEquals(1, repository.loads.size)
    }

    @Test fun searchesAreDebouncedAndLatestResultWins() = runTest(dispatcher) {
        auth.role = "admin"
        val model = model()
        advanceUntilIdle()
        model.onIntent(HomeIntent.Search("son"))
        advanceTimeBy(150)
        model.onIntent(HomeIntent.Search("sonido"))
        advanceUntilIdle()
        assertEquals(listOf("0:todas:sonido"), repository.requests)
        assertFalse(model.state.value.listLoading)
    }

    @Test fun paginationDeduplicatesAndStopsAtLastPage() = runTest(dispatcher) {
        auth.role = "admin"
        val model = model()
        advanceUntilIdle()
        model.onIntent(HomeIntent.LoadMore)
        model.onIntent(HomeIntent.LoadMore)
        advanceUntilIdle()
        assertEquals(listOf("50:todas:"), repository.requests)
        assertEquals(1, model.state.value.content.assignments.size)
        assertNull(model.state.value.content.nextOffset)
    }

    @Test fun approvalFailurePreservesPendingAccountAndRetrySucceeds() = runTest(dispatcher) {
        auth.role = "admin"
        val model = model()
        advanceUntilIdle()
        repository.failReview = true
        model.onIntent(HomeIntent.RequestReview(repository.account, true))
        model.onIntent(HomeIntent.ConfirmReview)
        advanceUntilIdle()
        assertEquals(1, model.state.value.content.dashboard?.pendingAccounts?.size)
        assertNotNull(model.state.value.actionFailure)
        repository.failReview = false
        model.onIntent(HomeIntent.RequestReview(repository.account, true))
        model.onIntent(HomeIntent.ConfirmReview)
        model.onIntent(HomeIntent.ConfirmReview)
        advanceUntilIdle()
        assertEquals(2, repository.reviews)
        assertEquals(0, model.state.value.content.dashboard?.pendingAccounts?.size)
    }

    @Test fun employeeHomeShowsAllAssignmentsAndCanFilterToday() = runTest(dispatcher) {
        val today = LocalDate.now()
        val date = today.atTime(10, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
        val tomorrow = today.plusDays(1).atTime(10, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
        repository.items = listOf(repository.assignment.copy(id = "today", scheduledDates = listOf(date)),
            repository.assignment.copy(id = "tomorrow", deadline = tomorrow))
        val model = model()
        advanceUntilIdle()
        assertEquals(2, model.state.value.visibleAssignments.size)
        model.onIntent(HomeIntent.ShowToday)
        assertEquals(listOf("today"), model.state.value.visibleAssignments.map { it.id })
    }

    @Test fun homeAppliesRealtimeDashboardChangesWithoutRefresh() = runTest(dispatcher) {
        auth.role = "admin"
        val model = model()
        advanceUntilIdle()
        val updated = HomeContent(repository.items, emptyList(),
            AdminDashboard(DashboardMetrics(23, 8, 4, 91.0, listOf(2, 3, 4)), emptyList()))

        repository.live.emit(updated)
        advanceUntilIdle()

        assertEquals(23, model.state.value.content.dashboard?.metrics?.total)
        assertTrue(model.state.value.content.dashboard?.pendingAccounts?.isEmpty() == true)
    }

    @Test fun performanceIsRestrictedToAdministrators() = runTest(dispatcher) {
        val employeeModel = model()
        advanceUntilIdle()
        employeeModel.onIntent(HomeIntent.OpenPerformance)
        advanceUntilIdle()
        assertEquals(ProfilePage.Overview, employeeModel.state.value.profilePage)
        assertEquals(0, admin.performanceLoads)

        auth.role = "admin"
        val adminModel = model()
        advanceUntilIdle()
        adminModel.onIntent(HomeIntent.OpenPerformance)
        advanceUntilIdle()
        assertEquals(ProfilePage.Performance, adminModel.state.value.profilePage)
        assertEquals(1, admin.performanceLoads)
        assertEquals("Ana Pérez", adminModel.state.value.performance.single().name)
    }

    @Test fun accessPanelLoadsAndReviewsAccountsForAdministratorsOnly() = runTest(dispatcher) {
        val employeeModel = model()
        advanceUntilIdle()
        employeeModel.onIntent(HomeIntent.OpenDashboard)
        employeeModel.onIntent(HomeIntent.RetryAccounts)
        advanceUntilIdle()
        assertEquals(0, admin.accountLoads)
        assertEquals(ProfilePage.Overview, employeeModel.state.value.profilePage)
        auth.role = "admin"
        val administrator = model()
        advanceUntilIdle()
        administrator.onIntent(HomeIntent.OpenDashboard)
        advanceUntilIdle()
        assertEquals(ProfilePage.Accounts, administrator.state.value.profilePage)
        assertEquals(AccountAccessState.Pending, administrator.state.value.accounts.single().state)
        administrator.onIntent(HomeIntent.ReviewManagedAccount("pending", AccountAccessState.Approved))
        advanceUntilIdle()
        assertEquals(AccountAccessState.Approved, administrator.state.value.accounts.single().state)
    }

    @Test fun performanceUsesFirstDayOfSelectedMonth() = runTest(dispatcher) {
        auth.role = "admin"
        val model = model()
        advanceUntilIdle()
        model.onIntent(HomeIntent.OpenPerformance)
        advanceUntilIdle()
        model.onIntent(HomeIntent.ChangePerformanceMonth(java.time.YearMonth.of(2026, 9)))
        advanceUntilIdle()
        assertEquals("2026-09-01", admin.lastMonth)
    }

    @Test fun complianceIncludesUnconfirmedAndEmptyPeriodHasNoPercentage() {
        val value = EmployeePerformance("id", "Ana", "Técnico", 1, 2, 1, 2, null, emptyList(), 0, 0, 0, 0)
        assertEquals(0.5, requireNotNull(value.compliance), 0.0001)
        assertNull(value.copy(early = 0, onTime = 0, late = 0, unconfirmed = 0).compliance)
    }

    private class TestAuth : AuthRepository {
        var role = "empleado"
        var access = AccountAccess.Approved
        override val session = MutableStateFlow<SessionState>(SessionState.SignedOut)
        override suspend fun login(email: String, password: String) { session.value = SessionState.SignedIn(AuthUser("user", email, "Ana")) }
        override suspend fun signUp(registration: Registration) { login(registration.email, registration.password) }
        override suspend fun requestPasswordRecovery(email: String) = Unit
        override suspend fun verifyPasswordRecovery(email: String, code: String) = Unit
        override suspend fun updateRecoveredPassword(password: String) = Unit
        override suspend fun cancelPasswordRecovery() = Unit
        override suspend fun changePassword(currentPassword: String, newPassword: String) = Unit
        override suspend fun signOut() { session.value = SessionState.SignedOut }
        override suspend fun deleteAccount() { session.value = SessionState.SignedOut }
        override suspend fun profile(userId: String): EmployeeProfile {
            delay(10)
            return EmployeeProfile(userId, "Ana Pérez", "ana", "", "Técnico", role, access)
        }
    }

    private class TestHome : HomeRepository {
        val loads = mutableListOf<Boolean>()
        val requests = mutableListOf<String>()
        val account = PendingAccount("pending", "Ana", "ana@empresa.com", "Técnico")
        val assignment = Assignment("assignment", "Montaje de sonido", "campo", "alta", "pendiente", "Hotel", null, null)
        var items = listOf(assignment)
        var failReview = false
        var reviews = 0
        val live = MutableSharedFlow<HomeContent>(extraBufferCapacity = 1)
        override suspend fun loadEmployeeAssignments(userId: String) = items
        override suspend fun loadDashboard() = AdminDashboard(DashboardMetrics(17, 5, 2, 98.4, listOf(1, 2, 1, 3, 2, 1, 1)), listOf(account))
        override suspend fun load(userId: String, administrator: Boolean): HomeContent {
            loads += administrator
            return HomeContent(items, emptyList(), if (administrator) loadDashboard() else null, if (administrator) 50 else null)
        }
        override suspend fun loadAdminAssignments(offset: Int, filter: String, search: String): AssignmentPage {
            requests += "$offset:$filter:$search"
            delay(20)
            return AssignmentPage(items, null)
        }
        override suspend fun reviewAccount(userId: String, approved: Boolean) {
            reviews++
            delay(10)
            if (failReview) error("No se pudo guardar")
        }
        override fun observe(userId: String, administrator: Boolean) = live
    }

    private class TestPreferences : LoginPreferences {
        override fun rememberedEmail() = ""
        override fun saveEmail(email: String?) = Unit
        override fun biometricEnabled(userId: String) = false
        override fun setBiometricEnabled(userId: String, enabled: Boolean) = Unit
    }

    private class TestAdmin : AdminRepository {
        var performanceLoads = 0
        var accountLoads = 0
        var lastMonth = ""
        var account = ManagedAccount("pending", "Ana", "ana@empresa.com", null, "Técnico", AccountAccessState.Pending, "2026-10-03T10:00:00Z")
        override suspend fun getDashboardMetrics() = DashboardMetrics(0, 0, 0, 0.0, emptyList())
        override suspend fun getAssignments(offset: Int, limit: Int, filter: AdminAssignmentFilter, search: String) =
            AdminAssignmentPage(emptyList(), false, null)
        override suspend fun getEmployees() = emptyList<Employee>()
        override suspend fun createAssignment(draft: AssignmentDraft) = error("No disponible")
        override suspend fun updateAssignment(id: String, draft: AssignmentDraft) = error("No disponible")
        override suspend fun deleteAssignment(id: String) = Unit
        override suspend fun getAccounts(): List<ManagedAccount> { accountLoads++; return listOf(account) }
        override suspend fun reviewAccount(userId: String, state: AccountAccessState) { account = account.copy(state = state) }
        override suspend fun getEmployeePerformance(month: String, employeeId: String?): List<EmployeePerformance> {
            performanceLoads++
            lastMonth = month
            return listOf(EmployeePerformance("employee", "Ana Pérez", "Técnico", 1, 3, 0, 0, 0.0, emptyList(), 4, 4, 0, 0))
        }
        override suspend fun getEmployeeAvailability(window: AssignmentBookingWindow, excludingAssignmentId: String?) =
            emptyList<EmployeeAvailability>()
    }
}
