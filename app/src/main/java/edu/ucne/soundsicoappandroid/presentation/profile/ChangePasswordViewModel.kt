package edu.ucne.soundsicoappandroid.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.usecase.ChangePasswordUseCase
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePasswordViewModel(private val changePassword: ChangePasswordUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow(ChangePasswordState())
    val state = mutableState.asStateFlow()

    fun onIntent(intent: ChangePasswordIntent) {
        if (state.value.saving) return
        when (intent) {
            is ChangePasswordIntent.CurrentChanged -> mutableState.update { it.copy(currentPassword = intent.value.take(72), errors = it.errors - "current") }
            is ChangePasswordIntent.PasswordChanged -> mutableState.update { it.copy(newPassword = intent.value.take(72), errors = it.errors - "password") }
            is ChangePasswordIntent.ConfirmationChanged -> mutableState.update { it.copy(confirmation = intent.value.take(72), errors = it.errors - "confirmation") }
            ChangePasswordIntent.Save -> save()
            ChangePasswordIntent.DismissError -> mutableState.update { it.copy(failure = null) }
        }
    }

    private fun save() {
        val input = state.value
        val errors = buildMap {
            if (input.currentPassword.isBlank()) put("current", "Introduce tu contraseña actual.")
            AuthValidation.password(input.newPassword)?.let { put("password", it) }
            if (input.currentPassword == input.newPassword && input.newPassword.isNotEmpty()) put("password", "La nueva contraseña debe ser diferente de la actual.")
            if (input.confirmation != input.newPassword) put("confirmation", "Las contraseñas deben coincidir exactamente.")
        }
        mutableState.update { it.copy(errors = errors, failure = null) }
        if (errors.isNotEmpty()) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                changePassword(input.currentPassword, input.newPassword)
                mutableState.update { it.copy(currentPassword = "", newPassword = "", confirmation = "", saved = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}
