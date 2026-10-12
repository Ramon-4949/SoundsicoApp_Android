package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.core.notifications.PushNotificationPayload
import org.junit.Assert.*
import org.junit.Test

class PushNotificationPayloadTest {
    private val owner = "00000000-0000-0000-0000-000000000001"
    private val data = mapOf(
        "notification_id" to "00000000-0000-0000-0000-000000000002",
        "recipient_id" to owner,
        "title" to "Hito vencido",
        "body" to "Confirma tu llegada al almacén."
    )

    @Test
    fun onlyAnExplicitTrueFlagSelectsAlarm() {
        assertTrue(requireNotNull(PushNotificationPayload.parse(data + ("is_alarm" to "true"), owner)).alarm)
        assertFalse(requireNotNull(PushNotificationPayload.parse(data + ("is_alarm" to "false"), owner)).alarm)
        assertFalse(requireNotNull(PushNotificationPayload.parse(data, owner)).alarm)
    }

    @Test
    fun falseAndMissingFlagsIgnoreLegacyAlarmHints() {
        val legacy = data + mapOf("sound" to "milestone_alarm.wav", "channel_id" to "soundisco_milestone_alarms")
        assertFalse(requireNotNull(PushNotificationPayload.parse(legacy + ("is_alarm" to "false"), owner)).alarm)
        assertFalse(requireNotNull(PushNotificationPayload.parse(legacy, owner)).alarm)
    }

    @Test
    fun serverReminderUsesStandardDeliveryAndPreservesRouting() {
        val reminder = data + mapOf(
            "is_alarm" to "false",
            "channel_id" to "soundisco_notifications",
            "title" to "Recordatorio de Hito"
        )
        val payload = requireNotNull(PushNotificationPayload.parse(reminder, owner))
        assertFalse(payload.alarm)
        assertEquals("Recordatorio de Hito", payload.title)
        assertEquals(data["notification_id"], payload.notificationId)
        assertEquals(owner, payload.recipientId)
    }

    @Test
    fun newAssignmentDoesNotRequireAnAlarmFlag() {
        val assignment = data + mapOf("title" to "Nueva asignación", "body" to "Te asignaron al montaje.")
        val payload = requireNotNull(PushNotificationPayload.parse(assignment, owner))
        assertFalse(payload.alarm)
        assertEquals("Nueva asignación", payload.title)
        assertEquals("Te asignaron al montaje.", payload.body)
    }

    @Test
    fun anotherAccountAndMalformedIdentifiersCannotTriggerAnAlarm() {
        assertNull(PushNotificationPayload.parse(data + ("is_alarm" to "true"), "00000000-0000-0000-0000-000000000003"))
        assertNull(PushNotificationPayload.parse(data + ("notification_id" to "invalid"), owner))
        assertNull(PushNotificationPayload.parse(data, null))
    }

    @Test
    fun legacyNotificationPayloadPreservesTextInTheForeground() {
        val payload = requireNotNull(PushNotificationPayload.parse(data - "title" - "body", owner, "Aviso", "Mensaje general"))
        assertEquals("Aviso", payload.title)
        assertEquals("Mensaje general", payload.body)
        assertFalse(payload.alarm)
    }
}
