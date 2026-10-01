package edu.ucne.soundsicoappandroid.presentation.login

data class LoginState(
    val email: String = "",
    val password: String = "",
    val rememberEmail: Boolean = false,
    val busy: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val failure: String? = null
)

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data class RememberChanged(val value: Boolean) : LoginIntent
    data object Submit : LoginIntent
    data object DismissError : LoginIntent
}
