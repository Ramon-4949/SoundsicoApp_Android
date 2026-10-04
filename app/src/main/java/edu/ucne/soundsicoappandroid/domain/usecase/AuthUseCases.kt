package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.model.Registration
import edu.ucne.soundsicoappandroid.domain.repository.AuthRepository
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) {
        require(AuthValidation.email(email) == null && password.isNotBlank()) { "Revisa tu correo y contraseña." }
        repository.login(email.trim(), password)
    }
}

class SignUpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(registration: Registration) {
        require(AuthValidation.registration(registration).isEmpty()) { "Revisa los datos del formulario." }
        repository.signUp(registration.copy(
            username = registration.username.trim(),
            fullName = registration.fullName.trim(),
            email = registration.email.trim(),
            phone = registration.phone.trim()
        ))
    }
}

class ObserveSessionUseCase(private val repository: AuthRepository) {
    operator fun invoke() = repository.session
}

class RequestPasswordRecoveryUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String) {
        require(AuthValidation.email(email) == null) { "Introduce un correo electrónico válido." }
        repository.requestPasswordRecovery(email.trim())
    }
}

class VerifyPasswordRecoveryUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String) {
        require(code.length == 6 && code.all(Char::isDigit)) { "Introduce el código de 6 dígitos." }
        repository.verifyPasswordRecovery(email, code)
    }
}

class UpdateRecoveredPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(password: String) {
        require(AuthValidation.password(password) == null) { "La contraseña no cumple los requisitos." }
        repository.updateRecoveredPassword(password)
    }
}

class CancelPasswordRecoveryUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.cancelPasswordRecovery()
}

class ChangePasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(currentPassword: String, newPassword: String) {
        require(currentPassword.isNotBlank()) { "Introduce tu contraseña actual." }
        require(AuthValidation.password(newPassword) == null) { "La nueva contraseña no cumple los requisitos." }
        require(currentPassword != newPassword) { "La nueva contraseña debe ser diferente de la actual." }
        repository.changePassword(currentPassword, newPassword)
    }
}

class GetProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String) = repository.profile(userId)
}

class SignOutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}

class DeleteAccountUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.deleteAccount()
}
