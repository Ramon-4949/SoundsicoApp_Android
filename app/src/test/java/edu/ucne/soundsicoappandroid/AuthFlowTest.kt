package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.*
import edu.ucne.soundsicoappandroid.domain.usecase.*
import edu.ucne.soundsicoappandroid.presentation.login.*
import edu.ucne.soundsicoappandroid.presentation.home.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = FakeAuth()
    private val preferences = FakePreferences()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun invalidLoginNeverCallsServer() = runTest(dispatcher) {
        val model = LoginViewModel(LoginUseCase(auth), preferences)
        model.onIntent(LoginIntent.Submit)
        advanceUntilIdle()
        assertEquals(0, auth.loginCalls)
        assertEquals(setOf("email", "password"), model.state.value.errors.keys)
    }

    @Test fun duplicateSubmitIsIgnoredAndPasswordIsCleared() = runTest(dispatcher) {
        val model = LoginViewModel(LoginUseCase(auth), preferences)
        model.onIntent(LoginIntent.EmailChanged(" ana@empresa.com "))
        model.onIntent(LoginIntent.PasswordChanged("Segura!928"))
        model.onIntent(LoginIntent.RememberChanged(true))
        model.onIntent(LoginIntent.Submit)
        model.onIntent(LoginIntent.Submit)
        advanceUntilIdle()
        assertEquals(1, auth.loginCalls)
        assertEquals("ana@empresa.com", auth.lastEmail)
        assertEquals("ana@empresa.com", preferences.email)
        assertEquals("", model.state.value.password)
        assertFalse(model.state.value.busy)
    }

    @Test fun serverFailureAllowsRetryAndDoesNotRememberEmail() = runTest(dispatcher) {
        auth.failure = IllegalStateException("Servicio no disponible")
        val model = LoginViewModel(LoginUseCase(auth), preferences)
        model.onIntent(LoginIntent.EmailChanged("ana@empresa.com"))
        model.onIntent(LoginIntent.PasswordChanged("Segura!928"))
        model.onIntent(LoginIntent.Submit)
        advanceUntilIdle()
        assertEquals("Servicio no disponible", model.state.value.failure)
        assertFalse(model.state.value.busy)
        assertEquals("", preferences.email)
        auth.failure = null
        model.onIntent(LoginIntent.Submit)
        advanceUntilIdle()
        assertEquals(2, auth.loginCalls)
        assertNull(model.state.value.failure)
    }

    @Test fun pendingAndRejectedAccountsCannotLoadHomeData() = runTest(dispatcher) {
        val home = FakeHome()
        val model = HomeViewModel(AuthUser("u", "ana@empresa.com", "Ana"),
            GetProfileUseCase(auth), GetHomeContentUseCase(home), SignOutUseCase(auth), GetEmployeePerformanceUseCase(FakeAdmin()),
            DeleteAccountUseCase(auth), preferences, ManageAccountsUseCase(FakeAdmin()))
        advanceUntilIdle()
        assertEquals(AccountAccess.Pending, model.state.value.profile?.access)
        assertEquals(0, home.calls)
        auth.access = AccountAccess.Rejected
        model.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()
        assertEquals(0, home.calls)
        auth.access = AccountAccess.Approved
        model.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()
        assertEquals(1, home.calls)
        auth.access = AccountAccess.Rejected
        model.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()
        assertTrue(model.state.value.content.assignments.isEmpty())
    }

    @Test fun signUpNormalizesMetadataAndNeverSuppliesRole() = runTest(dispatcher) {
        val input = Registration(" ana.perez ", " Ana Pérez ", " ana@empresa.com ",
            " 8095551234 ", "Técnico de sonido", "Segura!928", "Segura!928", true)
        SignUpUseCase(auth)(input)
        assertEquals("ana.perez", auth.registered?.username)
        assertEquals("Ana Pérez", auth.registered?.fullName)
        assertEquals("ana@empresa.com", auth.registered?.email)
        assertEquals("8095551234", auth.registered?.phone)
    }

    private class FakeAuth : AuthRepository {
        override val session = MutableStateFlow<SessionState>(SessionState.SignedOut)
        var loginCalls = 0
        var lastEmail = ""
        var failure: Exception? = null
        var access = AccountAccess.Pending
        var registered: Registration? = null
        override suspend fun login(email: String, password: String) {
            loginCalls++
            lastEmail = email
            delay(1)
            failure?.let { throw it }
        }
        override suspend fun signUp(registration: Registration) { registered = registration }
        override suspend fun requestPasswordRecovery(email: String) = Unit
        override suspend fun verifyPasswordRecovery(email: String, code: String) = Unit
        override suspend fun updateRecoveredPassword(password: String) = Unit
        override suspend fun cancelPasswordRecovery() = Unit
        override suspend fun changePassword(currentPassword: String, newPassword: String) = Unit
        override suspend fun signOut() { session.value = SessionState.SignedOut }
        override suspend fun deleteAccount() { session.value = SessionState.SignedOut }
        override suspend fun profile(userId: String) = EmployeeProfile(userId, "Ana", "ana", "8095551234", "Técnico de sonido", "empleado", access)
    }

    private class FakePreferences : LoginPreferences {
        var email = ""
        override fun rememberedEmail() = email
        override fun saveEmail(email: String?) { this.email = email.orEmpty() }
        override fun biometricEnabled(userId: String) = false
        override fun setBiometricEnabled(userId: String, enabled: Boolean) = Unit
    }

    private class FakeHome : HomeRepository {
        var calls = 0
        override suspend fun loadEmployeeAssignments(userId: String) = emptyList<Assignment>()
        override suspend fun loadDashboard() = AdminDashboard(DashboardMetrics(0, 0, 0, 0.0, emptyList()), emptyList())
        override suspend fun loadAdminAssignments(offset: Int, filter: String, search: String) = AssignmentPage(emptyList(), null)
        override suspend fun reviewAccount(userId: String, approved: Boolean) = Unit
        override suspend fun load(userId: String, administrator: Boolean): HomeContent {
            calls++
            return HomeContent(listOf(Assignment("a", "Evento", "campo", "alta", "pendiente", null, null, null)), emptyList())
        }
        override fun observe(userId: String, administrator: Boolean) = emptyFlow<HomeContent>()
    }

    private class FakeAdmin : AdminRepository {
        override suspend fun getDashboardMetrics() = DashboardMetrics(0, 0, 0, 0.0, emptyList())
        override suspend fun getAssignments(offset: Int, limit: Int, filter: AdminAssignmentFilter, search: String) =
            AdminAssignmentPage(emptyList(), false, null)
        override suspend fun getEmployees() = emptyList<Employee>()
        override suspend fun createAssignment(draft: AssignmentDraft) = error("No disponible")
        override suspend fun updateAssignment(id: String, draft: AssignmentDraft) = error("No disponible")
        override suspend fun deleteAssignment(id: String) = Unit
        override suspend fun getAccounts() = emptyList<ManagedAccount>()
        override suspend fun reviewAccount(userId: String, state: AccountAccessState) = Unit
        override suspend fun getEmployeePerformance(month: String, employeeId: String?) = emptyList<EmployeePerformance>()
        override suspend fun getEmployeeAvailability(window: AssignmentBookingWindow, excludingAssignmentId: String?) =
            emptyList<EmployeeAvailability>()
    }
}
