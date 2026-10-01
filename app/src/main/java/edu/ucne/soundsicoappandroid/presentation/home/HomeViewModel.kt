package edu.ucne.soundsicoappandroid.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.model.AccountAccess
import edu.ucne.soundsicoappandroid.domain.model.AuthUser
import edu.ucne.soundsicoappandroid.domain.model.HomeContent
import edu.ucne.soundsicoappandroid.domain.usecase.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    user: AuthUser,
    private val getProfile: GetProfileUseCase,
    private val getContent: GetHomeContentUseCase,
    private val signOut: SignOutUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeState(user))
    val state = mutableState.asStateFlow()
    private var refreshing = false

    init { refresh() }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refresh()
            HomeIntent.SignOut -> logout()
            is HomeIntent.SelectTab -> mutableState.update { it.copy(tab = intent.tab) }
            is HomeIntent.Search -> mutableState.update { it.copy(search = intent.value) }
            is HomeIntent.Filter -> mutableState.update { it.copy(filter = intent.value) }
            is HomeIntent.SelectDate -> mutableState.update { it.copy(selectedDate = intent.value) }
            is HomeIntent.OpenAssignment -> mutableState.update { it.copy(assignment = intent.value) }
            is HomeIntent.OpenBulletin -> mutableState.update { it.copy(bulletin = intent.value) }
            HomeIntent.CloseDetail -> mutableState.update { it.copy(assignment = null, bulletin = null) }
        }
    }

    private fun refresh() {
        if (refreshing || state.value.signingOut) return
        refreshing = true
        mutableState.update { it.copy(loading = true, failure = null) }
        viewModelScope.launch {
            try {
                val profile = getProfile(state.value.user.id)
                mutableState.update { it.copy(profile = profile, content = HomeContent(emptyList(), emptyList())) }
                if (profile.access == AccountAccess.Approved) {
                    val content = getContent(profile.id, profile.isAdministrator)
                    mutableState.update { it.copy(content = content) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                refreshing = false
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    private fun logout() {
        if (state.value.signingOut) return
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
}
