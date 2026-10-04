package edu.ucne.soundsicoappandroid.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.repository.ProfileRepository
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val userId: String,
    private val repository: ProfileRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(EditProfileState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun onIntent(intent: EditProfileIntent) {
        if (state.value.loading || state.value.saving) return
        when (intent) {
            is EditProfileIntent.UsernameChanged -> mutableState.update { it.copy(username = intent.value.take(30), errors = it.errors - "username") }
            is EditProfileIntent.PhoneChanged -> mutableState.update { it.copy(phone = intent.value.take(20), errors = it.errors - "phone") }
            is EditProfileIntent.PositionChanged -> mutableState.update { it.copy(position = intent.value, errors = it.errors - "position") }
            EditProfileIntent.Save -> save()
            EditProfileIntent.Retry -> load()
            EditProfileIntent.DismissError -> mutableState.update { it.copy(failure = null) }
        }
    }

    private fun load() {
        mutableState.update { it.copy(loading = true, failure = null) }
        viewModelScope.launch {
            try {
                val profile = repository.getProfile(userId)
                mutableState.update { it.copy(profile = profile, username = profile.username, phone = profile.phone, position = profile.position) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    private fun save() {
        val input = state.value
        val username = input.username.trim()
        val phone = input.phone.trim()
        val errors = buildMap {
            if (username.length !in 3..30 || !Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$").matches(username))
                put("username", "Usa entre 3 y 30 caracteres: letras, números, punto o guion.")
            if (phone.count(Char::isDigit) !in 10..15 || phone.any { !it.isDigit() && it !in "+() -" })
                put("phone", "Introduce un teléfono válido de 10 a 15 dígitos.")
            if (input.position !in AuthValidation.positions) put("position", "Selecciona un cargo válido.")
        }
        mutableState.update { it.copy(errors = errors, failure = null) }
        if (errors.isNotEmpty()) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val profile = repository.updateProfile(username, phone, input.position)
                mutableState.update { it.copy(profile = profile, username = profile.username, phone = profile.phone, position = profile.position, saved = true) }
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
