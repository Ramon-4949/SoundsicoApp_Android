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
    private val pageJobs = mutableMapOf<HomeTab, Job>()
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
            HomeIntent.RetryAssignments -> retryPage(HomeTab.Start)
            HomeIntent.RetryCalendar -> retryPage(HomeTab.Calendar)
            is HomeIntent.ChangePage -> if (intent.tab == HomeTab.Start || intent.tab == HomeTab.Calendar) {
                val list = state.value.listFor(intent.tab)
                if (!list.loading && (intent.offset == list.previousOffset || intent.offset == list.nextOffset)) {
                    loadPage(intent.tab, intent.offset)
                }
            }
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
                ensurePage(intent.tab)
            }
            is HomeIntent.Search -> changeSearch(intent.value.take(100))
            is HomeIntent.Filter -> if (state.value.filter != intent.value) {
                mutableState.update { it.copy(filter = intent.value, assignmentList = AssignmentListState()) }
                loadPage(HomeTab.Start)
            }
            is HomeIntent.SelectDate -> if (state.value.selectedDate != intent.value) {
                mutableState.update { it.copy(selectedDate = intent.value, calendarList = AssignmentListState()) }
                loadPage(HomeTab.Calendar)
            }
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
        if (state.value.search == value) return
        mutableState.update { it.copy(search = value, assignmentList = AssignmentListState()) }
        loadPage(HomeTab.Start, debounce = true)
    }

    private fun refresh() {
        if (refreshJob?.isActive == true || state.value.signingOut || state.value.savingAccount) return
        accountsJob?.cancel()
        cancelPageRequests()
        performanceJob?.cancel()
        homeObservation?.cancel()
        mutableState.update { it.copy(loading = true, failure = null,
            profile = null, content = HomeContent(), assignment = null, approvalsOpen = false, review = null,
            assignmentList = AssignmentListState(), calendarList = AssignmentListState(),
            accounts = emptyList(), accountsLoading = false, accountsFailure = null,
            profilePage = ProfilePage.Overview, performance = emptyList(), performanceLoading = false, performanceFailure = null) }
        refreshJob = viewModelScope.launch {
            try {
                val profile = getProfile(state.value.user.id)
                mutableState.update { it.copy(profile = profile) }
                if (profile.access == AccountAccess.Approved) {
                    ensurePage(state.value.tab)
                    val content = getContent(profile)
                    mutableState.update { it.copy(content = content) }
                    observeHome(profile)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    private fun ensurePage(tab: HomeTab) {
        if (tab != HomeTab.Start && tab != HomeTab.Calendar) return
        val page = state.value.listFor(tab)
        if (!page.loaded && !page.loading && page.failure == null) loadPage(tab)
    }

    private fun retryPage(tab: HomeTab) {
        val page = state.value.listFor(tab)
        if (!page.loading) loadPage(tab, page.offset)
    }

    private fun cancelPageRequests() {
        pageJobs.values.forEach { it.cancel() }
        pageJobs.clear()
    }

    private fun loadPage(tab: HomeTab, offset: Int = 0, debounce: Boolean = false, background: Boolean = false) {
        val input = state.value
        val profile = input.profile?.takeIf { it.access == AccountAccess.Approved } ?: return
        if (input.signingOut) return
        val current = input.listFor(tab)
        if (background && (current.loading || current.failure != null)) return
        pageJobs[tab]?.cancel()
        val pending = if (background) current.copy(loading = true) else AssignmentListState(offset = offset, loading = true)
        mutableState.update { it.withList(tab, pending) }
        pageJobs[tab] = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val page = if (tab == HomeTab.Calendar) {
                    getContent.assignmentPage(profile, offset, AssignmentFeedFilter.All, day = AssignmentDay.from(input.selectedDate))
                } else {
                    getContent.assignmentPage(profile, offset, input.filter.value, input.search)
                }
                if (isActive) mutableState.update {
                    it.withList(tab, AssignmentListState(page.items, offset, page.nextOffset, loaded = true))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (isActive) mutableState.update { it.withList(tab, pending.copy(loading = false, failure = error.userMessage())) }
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
                    mutableState.update { it.copy(content = live) }
                    val tab = state.value.tab
                    if (tab == HomeTab.Start || tab == HomeTab.Calendar) {
                        loadPage(tab, state.value.listFor(tab).offset, background = true)
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
        performanceJob?.cancel()
        homeObservation?.cancel()
        cancelPageRequests()
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
