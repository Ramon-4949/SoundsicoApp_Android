package edu.ucne.soundsicoappandroid.data.remote

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("nombre_completo") val name: String? = null,
    @SerialName("nombre_usuario") val username: String? = null,
    @SerialName("telefono") val phone: String? = null,
    @SerialName("cargo") val position: String? = null,
    @SerialName("rol") val role: String? = null
) {
    fun toDomain(access: AccountAccess) = EmployeeProfile(
        id, name.orEmpty(), username.orEmpty(), phone.orEmpty(), position.orEmpty(), role.orEmpty(), access
    )
}

@Serializable
data class AccessDto(val estado: String) {
    fun toDomain() = when (estado) {
        "aprobada" -> AccountAccess.Approved
        "rechazada" -> AccountAccess.Rejected
        else -> AccountAccess.Pending
    }
}
