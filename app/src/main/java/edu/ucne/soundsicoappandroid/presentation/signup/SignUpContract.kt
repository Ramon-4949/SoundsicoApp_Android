package edu.ucne.soundsicoappandroid.presentation.signup

import edu.ucne.soundsicoappandroid.domain.model.Registration
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation

enum class SignUpField { Username, FullName, Email, Phone, Position, Password, Confirmation }

data class SignUpState(
    val registration: Registration = Registration("", "", "", "", AuthValidation.positions.first(), "", "", false),
    val busy: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val failure: String? = null
)

sealed interface SignUpIntent {
    data class FieldChanged(val field: SignUpField, val value: String) : SignUpIntent
    data class TermsChanged(val accepted: Boolean) : SignUpIntent
    data object Submit : SignUpIntent
    data object DismissError : SignUpIntent
}
