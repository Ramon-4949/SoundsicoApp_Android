package edu.ucne.soundsicoappandroid.core.notifications

import java.util.UUID

data class PushNotificationPayload(
    val notificationId: String,
    val recipientId: String,
    val title: String,
    val body: String,
    val alarm: Boolean
) {
    companion object {
        fun parse(
            data: Map<String, String>,
            owner: String?,
            notificationTitle: String? = null,
            notificationBody: String? = null
        ): PushNotificationPayload? {
            val id = data["notification_id"] ?: return null
            val recipient = data["recipient_id"] ?: return null
            if (owner != recipient || runCatching { UUID.fromString(id); UUID.fromString(recipient) }.isFailure) return null
            val alarm = data["is_alarm"]?.equals("true", ignoreCase = true) == true
            return PushNotificationPayload(
                notificationId = id,
                recipientId = recipient,
                title = data["title"]?.takeIf { it.isNotBlank() } ?: notificationTitle ?: "SounDisco",
                body = data["body"]?.takeIf { it.isNotBlank() } ?: notificationBody ?: "Abre la app para revisar el aviso.",
                alarm = alarm
            )
        }
    }
}
