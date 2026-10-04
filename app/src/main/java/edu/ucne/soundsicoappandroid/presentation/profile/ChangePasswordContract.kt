package edu.ucne.soundsicoappandroid.presentation.profile

data class ChangePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val failure: String? = null
) {
    val passwordChecks: List<Pair<String, Boolean>> get() = listOf(
        "Mínimo 8 caracteres" to (newPassword.length in 8..72 && newPassword.none(Char::isWhitespace)),
        "Mayúscula y minúscula" to (newPassword.any(Char::isUpperCase) && newPassword.any(Char::isLowerCase)),
        "Al menos un número" to newPassword.any(Char::isDigit),
        "Símbolo especial (!@#$%)" to newPassword.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    )
}

sealed interface ChangePasswordIntent {
    data class CurrentChanged(val value: String) : ChangePasswordIntent
    data class PasswordChanged(val value: String) : ChangePasswordIntent
    data class ConfirmationChanged(val value: String) : ChangePasswordIntent
    data object Save : ChangePasswordIntent
    data object DismissError : ChangePasswordIntent
}
