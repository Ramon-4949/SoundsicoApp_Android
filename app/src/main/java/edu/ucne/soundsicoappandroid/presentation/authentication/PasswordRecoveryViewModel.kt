package edu.ucne.soundsicoappandroid.presentation.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.usecase.CancelPasswordRecoveryUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.RequestPasswordRecoveryUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.UpdateRecoveredPasswordUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.VerifyPasswordRecoveryUseCase
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PasswordRecoveryViewModel(
    initialEmail: String,
    private val requestRecovery: RequestPasswordRecoveryUseCase,
    private val verifyRecovery: VerifyPasswordRecoveryUseCase,
    private val updatePassword: UpdateRecoveredPasswordUseCase,
    private val cancelRecovery: CancelPasswordRecoveryUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(PasswordRecoveryState(email = initialEmail))
    val state = mutableState.asStateFlow()
    private var timer: Job? = null

    fun onIntent(intent: PasswordRecoveryIntent) {
        if (state.value.busy) return
        when (intent) {
            is PasswordRecoveryIntent.EmailChanged -> mutableState.update { it.copy(email = intent.value, errors = it.errors - "email") }
            is PasswordRecoveryIntent.DigitChanged -> updateDigit(intent.index, intent.value)
            is PasswordRecoveryIntent.PasswordChanged -> mutableState.update { it.copy(password = intent.value.take(72), errors = it.errors - "password") }
            is PasswordRecoveryIntent.ConfirmationChanged -> mutableState.update { it.copy(confirmation = intent.value.take(72), errors = it.errors - "confirmation") }
            PasswordRecoveryIntent.SubmitRequest -> sendCode()
            PasswordRecoveryIntent.Verify -> verifyCode()
            PasswordRecoveryIntent.Resend -> if (state.value.resendIn == 0) sendCode(resend = true)
            PasswordRecoveryIntent.SubmitPassword -> savePassword()
            PasswordRecoveryIntent.Previous -> previous()
            PasswordRecoveryIntent.DismissError -> mutableState.update { it.copy(failure = null) }
        }
    }

    private fun updateDigit(index: Int, value: String) {
        if (index !in 0..5) return
        val digits = state.value.code.padEnd(6).toCharArray()
        digits[index] = value.lastOrNull(Char::isDigit) ?: ' '
        mutableState.update { it.copy(code = String(digits).trimEnd(), errors = it.errors - "code") }
    }

    private fun sendCode(resend: Boolean = false) {
        val input = state.value
        val email = if (resend) input.sentEmail else input.email.trim()
        val emailError = AuthValidation.email(email)
        if (emailError != null) {
            mutableState.update { it.copy(errors = mapOf("email" to emailError)) }
            return
        }
        launchBusy {
            requestRecovery(email)
            mutableState.update { it.copy(step = PasswordRecoveryStep.Verification, sentEmail = email, code = "", expiresIn = 300, resendIn = 30, errors = emptyMap()) }
            startTimer()
        }
    }

    private fun verifyCode() {
        val input = state.value
        if (input.code.length != 6 || !input.code.all(Char::isDigit)) {
            mutableState.update { it.copy(errors = mapOf("code" to "Introduce el código completo de 6 dígitos.")) }
            return
        }
        launchBusy {
            verifyRecovery(input.sentEmail, input.code)
            timer?.cancel()
            mutableState.update { it.copy(step = PasswordRecoveryStep.Password, code = "", errors = emptyMap()) }
        }
    }

    private fun savePassword() {
        val input = state.value
        val errors = buildMap {
            AuthValidation.password(input.password)?.let { put("password", it) }
            if (input.confirmation != input.password) put("confirmation", "Las contraseñas deben coincidir exactamente.")
        }
        mutableState.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return
        launchBusy {
            updatePassword(input.password)
            mutableState.update { it.copy(step = PasswordRecoveryStep.Complete, password = "", confirmation = "", errors = emptyMap()) }
        }
    }

    private fun previous() {
        when (state.value.step) {
            PasswordRecoveryStep.Request -> Unit
            PasswordRecoveryStep.Verification -> {
                timer?.cancel()
                mutableState.update { it.copy(step = PasswordRecoveryStep.Request, code = "", errors = emptyMap()) }
            }
            PasswordRecoveryStep.Password -> viewModelScope.launch {
                cancelRecovery()
                mutableState.update { it.copy(step = PasswordRecoveryStep.Request, code = "", password = "", confirmation = "", errors = emptyMap()) }
            }
            PasswordRecoveryStep.Complete -> Unit
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (state.value.expiresIn > 0) {
                delay(1000)
                mutableState.update { it.copy(expiresIn = (it.expiresIn - 1).coerceAtLeast(0), resendIn = (it.resendIn - 1).coerceAtLeast(0)) }
            }
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        mutableState.update { it.copy(busy = true, failure = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    override fun onCleared() {
        timer?.cancel()
        super.onCleared()
    }
}
