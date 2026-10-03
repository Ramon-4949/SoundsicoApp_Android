package edu.ucne.soundsicoappandroid.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.LoginPreferences
import edu.ucne.soundsicoappandroid.domain.usecase.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.time.LocalDate

class HomeViewModel(
    user: AuthUser,
    private val getProfile: GetProfileUseCase,
    private val getContent: GetHomeContentUseCase,
    private val signOut: SignOutUseCase,
    private val getEmployeePerformance: GetEmployeePerformanceUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val preferences: LoginPreferences,
    private val manageAccounts: ManageAccountsUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeState(user, biometricEnabled = preferences.biometricEnabled(user.id)))
    val state = mutableState.asStateFlow()
    private var refreshJob: Job? = null
    private var pageJob: Job? = null
    private var pageGeneration = 0
    private var calendarJob: Job? = null
    private var performanceJob: Job? = null
    private var accountsJob: Job? = null
    private var homeObservation: Job? = null

    init { refresh() }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.OpenNotifications -> mutableState.update { it.copy(notificationsOpen = true) }
            HomeIntent.CloseNotifications -> mutableState.update { it.copy(notificationsOpen = false) }
            HomeIntent.Refresh -> refresh()
            HomeIntent.SignOut -> logout()
            HomeIntent.ToggleSearch -> {
                mutableState.update { it.copy(searching = !it.searching) }
                if (!state.value.searching) changeSearch("")
            }
            HomeIntent.ShowAll -> {
                mutableState.update { it.copy(todayOnly = false, search = "", filter = AssignmentFilter.All) }
                loadPage()
            }
            HomeIntent.ShowToday -> mutableState.update { it.copy(todayOnly = true, today = LocalDate.now()) }
            HomeIntent.LoadMore -> loadPage(append = true)
            HomeIntent.RetryAssignments -> loadPage()
            HomeIntent.OpenApprovals -> if (state.value.audience == HomeAudience.Administrator)
                mutableState.update { it.copy(approvalsOpen = true) }
            HomeIntent.CloseApprovals -> if (!state.value.savingAccount) mutableState.update { it.copy(approvalsOpen = false, review = null) }
            is HomeIntent.RequestReview -> if (state.value.audience == HomeAudience.Administrator && !state.value.savingAccount &&
                state.value.content.dashboard?.pendingAccounts?.any { it.id == intent.account.id } == true)
                mutableState.update { it.copy(review = AccountReview(intent.account, intent.approved)) }
            HomeIntent.ConfirmReview -> reviewAccount()
            HomeIntent.CancelReview -> if (!state.value.savingAccount) mutableState.update { it.copy(review = null) }
            HomeIntent.DismissActionError -> mutableState.update { it.copy(actionFailure = null) }
            is HomeIntent.SelectTab -> {
                mutableState.update {
                    it.copy(tab = intent.tab, profilePage = if (intent.tab == HomeTab.Profile) it.profilePage else ProfilePage.Overview)
                }
                if (intent.tab == HomeTab.Calendar) loadCalendar()
            }
            is HomeIntent.Search -> changeSearch(intent.value.take(100))
            is HomeIntent.Filter -> {
                mutableState.update { it.copy(filter = intent.value) }
                loadPage()
            }
            is HomeIntent.SelectDate -> mutableState.update { it.copy(selectedDate = intent.value) }
            is HomeIntent.OpenAssignment -> mutableState.update { it.copy(assignment = intent.value) }
            is HomeIntent.OpenBulletin -> mutableState.update { it.copy(bulletin = intent.value) }
            HomeIntent.OpenDashboard -> if (state.value.audience == HomeAudience.Administrator) {
                mutableState.update { it.copy(profilePage = ProfilePage.Accounts) }
                loadAccounts()
            }
            HomeIntent.RetryAccounts -> loadAccounts()
            is HomeIntent.ReviewManagedAccount -> reviewManagedAccount(intent)
            is HomeIntent.ChangePerformanceMonth -> {
                performanceJob?.cancel()
                mutableState.update { it.copy(performanceMonth = intent.month, performance = emptyList()) }
                loadPerformance()
            }
            HomeIntent.OpenPerformance -> if (state.value.audience == HomeAudience.Administrator) {
                mutableState.update { it.copy(profilePage = ProfilePage.Performance) }
                loadPerformance()
            }
            HomeIntent.ClosePerformance -> mutableState.update { it.copy(profilePage = ProfilePage.Overview) }
            HomeIntent.RetryPerformance -> loadPerformance()
            is HomeIntent.SetBiometric -> {
                preferences.setBiometricEnabled(state.value.user.id, intent.enabled)
                mutableState.update { it.copy(biometricEnabled = intent.enabled) }
            }
            HomeIntent.RequestDeleteAccount -> mutableState.update { it.copy(deleteAccountConfirmation = true) }
            HomeIntent.CancelDeleteAccount -> if (!state.value.deletingAccount)
                mutableState.update { it.copy(deleteAccountConfirmation = false) }
            HomeIntent.ConfirmDeleteAccount -> removeAccount()
            HomeIntent.CloseDetail -> mutableState.update { it.copy(assignment = null, bulletin = null) }
        }
    }

    private fun changeSearch(value: String) {
        mutableState.update { it.copy(search = value) }
        loadPage(debounce = true)
    }

    private fun refresh() {
        if (refreshJob?.isActive == true || state.value.signingOut || state.value.savingAccount) return
        accountsJob?.cancel()
        pageJob?.cancel()
        calendarJob?.cancel()
        performanceJob?.cancel()
        homeObservation?.cancel()
        pageGeneration++
        mutableState.update { it.copy(loading = true, failure = null, listFailure = null,
            listLoading = false, loadingMore = false, today = LocalDate.now(),
            profile = null, content = HomeContent(emptyList(), emptyList()), assignment = null, approvalsOpen = false, review = null,
            calendarAssignments = null, calendarLoading = false, calendarFailure = null,
            accounts = emptyList(), accountsLoading = false, accountsFailure = null,
            profilePage = ProfilePage.Overview, performance = emptyList(), performanceLoading = false, performanceFailure = null) }
        refreshJob = viewModelScope.launch {
            try {
                val profile = getProfile(state.value.user.id)
                mutableState.update { it.copy(profile = profile) }
                if (profile.access == AccountAccess.Approved) {
                    val content = getContent(profile)
                    mutableState.update { it.copy(content = content) }
                    if (profile.isAdministrator && (state.value.filter != AssignmentFilter.All || state.value.search.isNotBlank())) {
                        val page = getContent.adminAssignments(profile, 0, state.value.filter.serverValue, state.value.search)
                        mutableState.update { it.copy(content = it.content.copy(assignments = page.items, nextOffset = page.nextOffset)) }
                    }
                    observeHome(profile)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(loading = false) }
                if (isActive && state.value.tab == HomeTab.Calendar) loadCalendar()
            }
        }
    }

    private fun loadPage(append: Boolean = false, debounce: Boolean = false) {
        val input = state.value
        if (input.audience != HomeAudience.Administrator || input.loading || input.signingOut) return
        if (append && (input.loadingMore || input.listLoading || input.content.nextOffset == null)) return
        pageJob?.cancel()
        val generation = ++pageGeneration
        val offset = if (append) input.content.nextOffset ?: return else 0
        mutableState.update { it.copy(listLoading = !append, loadingMore = append, listFailure = null,
            content = if (append) it.content else it.content.copy(assignments = emptyList(), nextOffset = null)) }
        pageJob = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val page = getContent.adminAssignments(requireNotNull(input.profile), offset, input.filter.serverValue, input.search)
                if (generation == pageGeneration) mutableState.update {
                    it.copy(content = it.content.copy(
                        assignments = (if (append) it.content.assignments + page.items else page.items).distinctBy(Assignment::id),
                        nextOffset = page.nextOffset
                    ))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation == pageGeneration) mutableState.update { it.copy(listFailure = error.userMessage()) }
            } finally {
                if (generation == pageGeneration) mutableState.update { it.copy(listLoading = false, loadingMore = false) }
            }
        }
    }

    private fun observeHome(profile: EmployeeProfile) {
        homeObservation?.cancel()
        homeObservation = viewModelScope.launch {
            getContent.observe(profile)
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(actionFailure = error.userMessage()) }
                }
                .collectLatest { live ->
                    if (profile.isAdministrator && (state.value.filter != AssignmentFilter.All || state.value.search.isNotBlank())) {
                        val page = getContent.adminAssignments(profile, 0, state.value.filter.serverValue, state.value.search)
                        mutableState.update {
                            it.copy(content = live.copy(assignments = page.items, nextOffset = page.nextOffset))
                        }
                    } else {
                        mutableState.update { it.copy(content = live) }
                    }
                    if (state.value.profilePage == ProfilePage.Performance) loadPerformance()
                    if (state.value.profilePage == ProfilePage.Accounts) loadAccounts()
                }
        }
    }

    private fun reviewAccount() {
        val input = state.value
        val review = input.review ?: return
        if (input.audience != HomeAudience.Administrator || input.savingAccount) return
        mutableState.update { it.copy(savingAccount = true, actionFailure = null) }
        viewModelScope.launch {
            try {
                getContent.reviewAccount(requireNotNull(input.profile), review.account.id, review.approved)
                mutableState.update { current ->
                    current.copy(review = null, content = current.content.copy(dashboard = current.content.dashboard?.let {
                        it.copy(pendingAccounts = it.pendingAccounts.filterNot { account -> account.id == review.account.id })
                    }))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(review = null, actionFailure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(savingAccount = false) }
            }
        }
    }

    private fun loadCalendar() {
        if (state.value.audience != HomeAudience.Administrator || state.value.loading || state.value.signingOut ||
            state.value.calendarAssignments != null || calendarJob?.isActive == true) return
        mutableState.update { it.copy(calendarLoading = true, calendarFailure = null) }
        calendarJob = viewModelScope.launch {
            try {
                val assignments = getContent.adminCalendar(requireNotNull(state.value.profile))
                mutableState.update { it.copy(calendarAssignments = assignments) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(calendarFailure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(calendarLoading = false) }
            }
        }
    }

    private fun loadPerformance() {
        if (state.value.audience != HomeAudience.Administrator || performanceJob?.isActive == true) return
        mutableState.update { it.copy(performanceLoading = true, performanceFailure = null) }
        performanceJob = viewModelScope.launch {
            try {
                val values = getEmployeePerformance(requireNotNull(state.value.profile), state.value.performanceMonth.atDay(1).toString())
                mutableState.update { it.copy(performance = values) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(performanceFailure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(performanceLoading = false) }
            }
        }
    }

    private fun loadAccounts() {
        if (state.value.audience != HomeAudience.Administrator || accountsJob?.isActive == true) return
        accountsJob = viewModelScope.launch {
            mutableState.update { it.copy(accountsLoading = true, accountsFailure = null) }
            try {
                val accounts = manageAccounts.load(requireNotNull(state.value.profile))
                mutableState.update { it.copy(accounts = accounts) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(accountsFailure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(accountsLoading = false) }
            }
        }
    }

    private fun reviewManagedAccount(intent: HomeIntent.ReviewManagedAccount) {
        if (state.value.audience != HomeAudience.Administrator || state.value.reviewingAccount != null) return
        if (state.value.accounts.none { it.id == intent.id }) return
        mutableState.update { it.copy(reviewingAccount = intent.id, accountsFailure = null) }
        viewModelScope.launch {
            try {
                manageAccounts.review(requireNotNull(state.value.profile), intent.id, intent.access)
                accountsJob?.cancel()
                mutableState.update { current -> current.copy(accounts = current.accounts.map {
                    if (it.id == intent.id) it.copy(state = intent.access) else it
                }) }
                loadAccounts()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(accountsFailure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(reviewingAccount = null) }
            }
        }
    }

    private fun logout() {
        if (state.value.signingOut || state.value.savingAccount) return
        accountsJob?.cancel()
        refreshJob?.cancel()
        calendarJob?.cancel()
        performanceJob?.cancel()
        homeObservation?.cancel()
        pageJob?.cancel()
        pageGeneration++
        mutableState.update { it.copy(signingOut = true, failure = null) }
        viewModelScope.launch {
            try {
                signOut()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(signingOut = false) }
            }
        }
    }

    private fun removeAccount() {
        if (state.value.deletingAccount || state.value.signingOut) return
        mutableState.update { it.copy(deletingAccount = true, deleteAccountConfirmation = false, actionFailure = null) }
        viewModelScope.launch {
            try {
                deleteAccount()
                preferences.setBiometricEnabled(state.value.user.id, false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(actionFailure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(deletingAccount = false) }
            }
        }
    }
}
