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

class GetProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: String) = repository.profile(userId)
}

class SignOutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}

class DeleteAccountUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.deleteAccount()
}
