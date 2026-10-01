package edu.ucne.soundsicoappandroid.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.repository.LoginPreferences
import edu.ucne.soundsicoappandroid.domain.usecase.LoginUseCase
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val login: LoginUseCase,
    private val preferences: LoginPreferences
) : ViewModel() {
    private val remembered = preferences.rememberedEmail()
    private val mutableState = MutableStateFlow(LoginState(email = remembered, rememberEmail = remembered.isNotEmpty()))
    val state = mutableState.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        if (state.value.busy) return
        when (intent) {
            is LoginIntent.EmailChanged -> mutableState.update { it.copy(email = intent.value, errors = it.errors - "email", failure = null) }
            is LoginIntent.PasswordChanged -> mutableState.update { it.copy(password = intent.value, errors = it.errors - "password", failure = null) }
            is LoginIntent.RememberChanged -> {
                if (!intent.value) preferences.saveEmail(null)
                mutableState.update { it.copy(rememberEmail = intent.value) }
            }
            LoginIntent.DismissError -> mutableState.update { it.copy(failure = null) }
            LoginIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val input = state.value
        val errors = buildMap {
            AuthValidation.email(input.email)?.let { put("email", it) }
            if (input.password.isBlank()) put("password", "La contraseña es obligatoria.")
        }
        mutableState.update { it.copy(errors = errors, failure = null) }
        if (errors.isNotEmpty()) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                login(input.email, input.password)
                preferences.saveEmail(input.email.trim().takeIf { input.rememberEmail })
                mutableState.update { it.copy(password = "") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
