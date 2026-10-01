package edu.ucne.soundsicoappandroid.domain.model

data class AuthUser(val id: String, val email: String, val displayName: String)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}

data class Registration(
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val position: String,
    val password: String,
    val confirmation: String,
    val acceptedTerms: Boolean
)

enum class AccountAccess { Pending, Approved, Rejected }

data class EmployeeProfile(
    val id: String,
    val name: String,
    val username: String,
    val phone: String,
    val position: String,
    val role: String,
    val access: AccountAccess
) {
    val isAdministrator: Boolean get() = role == "admin"
}
