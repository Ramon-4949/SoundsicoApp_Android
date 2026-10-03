package edu.ucne.soundsicoappandroid.presentation.admindashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile
import edu.ucne.soundsicoappandroid.domain.usecase.GetEmployeePerformanceUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.ObserveAdminDashboardUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.ReviewPendingAccountUseCase
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDashboardViewModel(
    private val profile: EmployeeProfile,
    private val observeDashboard: ObserveAdminDashboardUseCase,
    private val reviewAccount: ReviewPendingAccountUseCase,
    private val getPerformance: GetEmployeePerformanceUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminDashboardState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        observe()
    }

    fun onIntent(intent: AdminDashboardIntent) {
        when (intent) {
            AdminDashboardIntent.Refresh -> observe()
            is AdminDashboardIntent.ReviewAccount -> review(intent)
            AdminDashboardIntent.DismissError -> mutableState.update { it.copy(failure = null) }
        }
    }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, failure = null) }
            observeDashboard(profile)
                .map { dashboard -> dashboard to getPerformance(profile, YearMonth.now().toString()) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(loading = false, failure = error.userMessage()) }
                }
                .collect { (dashboard, performance) ->
                    mutableState.update {
                        it.copy(dashboard = dashboard, performance = performance, loading = false, failure = null)
                    }
                }
        }
    }

    private fun review(intent: AdminDashboardIntent.ReviewAccount) {
        if (state.value.reviewingAccountId != null) return
        if (state.value.dashboard?.pendingAccounts?.none { it.id == intent.account.id } != false) return
        mutableState.update { it.copy(reviewingAccountId = intent.account.id, failure = null) }
        viewModelScope.launch {
            try {
                reviewAccount(profile, intent.account.id, intent.approved)
                mutableState.update { current ->
                    current.copy(dashboard = current.dashboard?.copy(
                        pendingAccounts = current.dashboard.pendingAccounts.filterNot { it.id == intent.account.id }
                    ))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(reviewingAccountId = null) }
            }
        }
    }
}
