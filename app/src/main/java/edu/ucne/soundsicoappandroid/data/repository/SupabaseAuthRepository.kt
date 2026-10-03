package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.AccessDto
import edu.ucne.soundsicoappandroid.data.remote.ProfileDto
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseAuthRepository(private val client: SupabaseClient, private val push: edu.ucne.soundsicoappandroid.core.notifications.PushRegistration) : AuthRepository {
    override val session = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Initializing -> SessionState.Loading
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                if (user == null) SessionState.SignedOut else SessionState.SignedIn(
                    AuthUser(user.id, user.email.orEmpty(),
                        user.userMetadata?.get("nombre_usuario")?.jsonPrimitive?.contentOrNull
                            ?: user.userMetadata?.get("nombre_completo")?.jsonPrimitive?.contentOrNull
                            ?: user.email.orEmpty().substringBefore("@"))
                )
            }
            else -> SessionState.SignedOut
        }
    }.distinctUntilChanged()

    override suspend fun login(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(registration: Registration) {
        client.auth.signUpWith(Email) {
            email = registration.email
            password = registration.password
            data = buildJsonObject {
                put("nombre_usuario", registration.username)
                put("nombre_completo", registration.fullName)
                put("telefono", registration.phone)
                put("cargo", registration.position)
            }
        }
        check(client.auth.currentSessionOrNull() != null) {
            "El servidor no entregó una sesión. Revisa tu correo y la configuración de confirmación de cuentas."
        }
    }

    override suspend fun signOut() {
        push.disconnect()
        client.auth.signOut()
    }

    override suspend fun deleteAccount() {
        client.postgrest.rpc("delete_my_account")
        push.clear()
        client.auth.signOut()
    }

    override suspend fun profile(userId: String): EmployeeProfile {
        val access = client.postgrest.rpc("my_account_access").decodeAs<AccessDto>().toDomain()
        return client.from("perfiles").select { filter { eq("id", userId) } }
            .decodeSingle<ProfileDto>().toDomain(access)
    }
}
