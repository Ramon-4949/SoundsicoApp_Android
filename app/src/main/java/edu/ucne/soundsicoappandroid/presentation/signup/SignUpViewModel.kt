package edu.ucne.soundsicoappandroid.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.usecase.SignUpUseCase
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(private val signUp: SignUpUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow(SignUpState())
    val state = mutableState.asStateFlow()

    fun onIntent(intent: SignUpIntent) {
        if (state.value.busy) return
        when (intent) {
            is SignUpIntent.FieldChanged -> mutableState.update { current ->
                val form = current.registration
                current.copy(registration = when (intent.field) {
                    SignUpField.Username -> form.copy(username = intent.value)
                    SignUpField.FullName -> form.copy(fullName = intent.value)
                    SignUpField.Email -> form.copy(email = intent.value)
                    SignUpField.Phone -> form.copy(phone = intent.value)
                    SignUpField.Position -> form.copy(position = intent.value)
                    SignUpField.Password -> form.copy(password = intent.value)
                    SignUpField.Confirmation -> form.copy(confirmation = intent.value)
                }, errors = emptyMap(), failure = null)
            }
            is SignUpIntent.TermsChanged -> mutableState.update {
                it.copy(registration = it.registration.copy(acceptedTerms = intent.accepted), errors = it.errors - "terms")
            }
            SignUpIntent.DismissError -> mutableState.update { it.copy(failure = null) }
            SignUpIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val form = state.value.registration
        val errors = AuthValidation.registration(form)
        mutableState.update { it.copy(errors = errors, failure = null) }
        if (errors.isNotEmpty()) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                signUp(form)
                mutableState.update { it.copy(registration = it.registration.copy(password = "", confirmation = "")) }
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
