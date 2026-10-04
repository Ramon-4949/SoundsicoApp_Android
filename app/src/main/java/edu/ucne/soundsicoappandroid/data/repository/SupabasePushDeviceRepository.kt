package edu.ucne.soundsicoappandroid.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabasePushDeviceRepository(private val client: SupabaseClient) {
    suspend fun register(installation: String, token: String) {
        client.postgrest.rpc("register_android_push_device", buildJsonObject {
            put("p_installation", installation)
            put("p_token", token)
        })
    }

    suspend fun unregister(installation: String) {
        client.postgrest.rpc("unregister_android_push_device", buildJsonObject {
            put("p_installation", installation)
        })
    }
}
