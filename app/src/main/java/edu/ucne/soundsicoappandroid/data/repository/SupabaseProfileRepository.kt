package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.ProfileDto
import edu.ucne.soundsicoappandroid.data.remote.AccessDto
import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile
import edu.ucne.soundsicoappandroid.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseProfileRepository(private val client: SupabaseClient) : ProfileRepository {
    override suspend fun getProfile(userId: String): EmployeeProfile =
        client.from("perfiles").select {
            filter { eq("id", userId) }
            limit(1)
        }.decodeSingle<ProfileDto>().toDomain(
            client.postgrest.rpc("my_account_access").decodeAs<AccessDto>().toDomain()
        )

    override suspend fun updateProfile(username: String, phone: String, position: String): EmployeeProfile {
        client.postgrest.rpc("update_my_profile", buildJsonObject {
            put("p_username", username.trim())
            put("p_phone", phone.trim())
            put("p_job", position)
        })
        return getProfile(requireNotNull(client.auth.currentUserOrNull()).id)
    }
}
