package edu.ucne.soundsicoappandroid

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.messaging.RemoteMessage
import edu.ucne.soundsicoappandroid.core.notifications.NotificationChannels
import edu.ucne.soundsicoappandroid.core.notifications.SoundiscoMessagingService
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class PushNotificationDeliveryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun falseReminderUsesNormalNotificationInTheForeground() {
        compose.setContent { Text("Prueba de recepción") }
        compose.waitUntil { compose.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        checkDelivery(mapOf("is_alarm" to "false", "title" to "Recordatorio de Hito"))
    }

    @Test
    fun falseReminderUsesNormalNotificationInTheBackground() {
        compose.setContent { Text("Prueba de recepción") }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
        compose.waitUntil { !compose.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        checkDelivery(mapOf("is_alarm" to "false", "title" to "Recordatorio de Hito"))
    }

    @Test
    fun newAssignmentWithoutFlagStillArrivesNormally() {
        compose.setContent { Text("Prueba de recepción") }
        checkDelivery(mapOf("title" to "Nueva asignación"))
    }

    private fun checkDelivery(extra: Map<String, String>) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val preferences = context.getSharedPreferences("soundisco_push", Context.MODE_PRIVATE)
        val previousOwner = preferences.getString("owner", null)
        val recipient = UUID.randomUUID().toString()
        val id = UUID.randomUUID().toString()
        val manager = context.getSystemService(NotificationManager::class.java)
        preferences.edit().putString("owner", recipient).commit()
        try {
            val service = SoundiscoMessagingService()
            ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply {
                isAccessible = true
                invoke(service, context)
            }
            val data = mapOf(
                "notification_id" to id,
                "recipient_id" to recipient,
                "body" to "Aviso de prueba",
                "channel_id" to NotificationChannels.OPERATIONS
            ) + extra
            val message = RemoteMessage.Builder("test-sender").setData(data).build()
            service.onMessageReceived(message)
            compose.waitUntil { manager.activeNotifications.any { it.tag == id } }
            val notification = manager.activeNotifications.single { it.tag == id }.notification
            assertEquals(extra["title"], notification.extras.getString(Notification.EXTRA_TITLE))
            if (Build.VERSION.SDK_INT >= 26) assertEquals(NotificationChannels.OPERATIONS, notification.channelId)
            assertNull(notification.fullScreenIntent)
            assertEquals(Notification.CATEGORY_EVENT, notification.category)
            assertEquals(0, notification.flags and Notification.FLAG_INSISTENT)
            manager.cancel(id, 0)
            compose.waitUntil { manager.activeNotifications.none { it.tag == id } }
            service.onMessageReceived(message)
            assertTrue(manager.activeNotifications.none { it.tag == id })
        } finally {
            manager.cancel(id, 0)
            preferences.edit().apply {
                if (previousOwner == null) remove("owner") else putString("owner", previousOwner)
            }.commit()
        }
    }
}
