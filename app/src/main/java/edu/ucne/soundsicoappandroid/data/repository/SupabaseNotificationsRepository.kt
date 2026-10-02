package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.EmployeeNotificationDto
import edu.ucne.soundsicoappandroid.domain.model.EmployeeNotification
import edu.ucne.soundsicoappandroid.domain.repository.NotificationsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.put

class SupabaseNotificationsRepository(private val client: SupabaseClient) : NotificationsRepository {
    override suspend fun getNotifications(userId: String): List<EmployeeNotification> {
        val result = mutableListOf<EmployeeNotificationDto>()
        var offset = 0L
        do {
            val page = client.from("notificaciones_app").select {
                filter { eq("perfil_id", userId) }
                order("fecha_creacion", Order.DESCENDING)
                order("id", Order.ASCENDING)
                range(offset, offset + 199)
            }.decodeList<EmployeeNotificationDto>()
            result += page
            offset += 200
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    override suspend fun markRead(id: String?) {
        client.postgrest.rpc("notifications_mark_read", buildJsonObject {
            id?.let { put("p_id", it) } ?: put("p_id", JsonNull)
        })
    }

    override fun observeNotifications(userId: String): Flow<List<EmployeeNotification>> = flow {
        val channel = client.channel("notifications-$userId-${System.nanoTime()}")
        val changes = channel.postgresChangeFlow<PostgresAction>("public") {
            table = "notificaciones_app"
            filter("perfil_id", FilterOperator.EQ, userId)
        }
        try {
            emit(getNotifications(userId))
            channel.subscribe(true)
            val polling = flow {
                while (true) {
                    delay(30_000)
                    emit(Unit)
                }
            }
            merge(changes.map { Unit }, polling).collect {
                emit(getNotifications(userId))
            }
        } finally {
            client.realtime.removeChannel(channel)
        }
    }
}
