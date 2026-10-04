package edu.ucne.soundsicoappandroid.presentation.authentication

enum class PasswordRecoveryStep { Request, Verification, Password, Complete }

data class PasswordRecoveryState(
    val step: PasswordRecoveryStep = PasswordRecoveryStep.Request,
    val email: String = "",
    val sentEmail: String = "",
    val code: String = "",
    val password: String = "",
    val confirmation: String = "",
    val expiresIn: Int = 300,
    val resendIn: Int = 0,
    val busy: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val failure: String? = null
) {
    val passwordChecks: List<Pair<String, Boolean>> get() = listOf(
        "Mínimo 8 caracteres" to (password.length in 8..72 && password.none(Char::isWhitespace)),
        "Mayúscula y minúscula" to (password.any(Char::isUpperCase) && password.any(Char::isLowerCase)),
        "Al menos un número" to password.any(Char::isDigit),
        "Símbolo especial (!@#$%)" to password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    )
}

sealed interface PasswordRecoveryIntent {
    data class EmailChanged(val value: String) : PasswordRecoveryIntent
    data class DigitChanged(val index: Int, val value: String) : PasswordRecoveryIntent
    data class PasswordChanged(val value: String) : PasswordRecoveryIntent
    data class ConfirmationChanged(val value: String) : PasswordRecoveryIntent
    data object SubmitRequest : PasswordRecoveryIntent
    data object Verify : PasswordRecoveryIntent
    data object Resend : PasswordRecoveryIntent
    data object SubmitPassword : PasswordRecoveryIntent
    data object Previous : PasswordRecoveryIntent
    data object DismissError : PasswordRecoveryIntent
}
