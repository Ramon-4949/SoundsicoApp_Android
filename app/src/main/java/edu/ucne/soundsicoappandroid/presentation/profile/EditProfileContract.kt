package edu.ucne.soundsicoappandroid.presentation.profile

import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile

data class EditProfileState(
    val profile: EmployeeProfile? = null,
    val username: String = "",
    val phone: String = "",
    val position: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val failure: String? = null
) {
    val changed: Boolean get() = profile?.let {
        username != it.username || phone != it.phone || position != it.position
    } == true
}

sealed interface EditProfileIntent {
    data class UsernameChanged(val value: String) : EditProfileIntent
    data class PhoneChanged(val value: String) : EditProfileIntent
    data class PositionChanged(val value: String) : EditProfileIntent
    data object Save : EditProfileIntent
    data object Retry : EditProfileIntent
    data object DismissError : EditProfileIntent
}
