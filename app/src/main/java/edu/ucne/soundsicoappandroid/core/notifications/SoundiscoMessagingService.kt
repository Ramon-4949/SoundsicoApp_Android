package edu.ucne.soundsicoappandroid.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import edu.ucne.soundsicoappandroid.R
import edu.ucne.soundsicoappandroid.app.MainActivity

class SoundiscoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushRegistration.enqueue(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val owner = getSharedPreferences("soundisco_push", MODE_PRIVATE).getString("owner", null)
        val payload = PushNotificationPayload.parse(
            message.data, owner, message.notification?.title, message.notification?.body, message.notification?.channelId
        ) ?: return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(NotificationManager::class.java)
        NotificationChannels.create(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = android.net.Uri.parse("soundisco://notification/${payload.notificationId}")
            putExtra("notification_id", payload.notificationId)
            putExtra("recipient_id", payload.recipientId)
        }
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, if (payload.alarm) NotificationChannels.ALARMS else CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(if (payload.alarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (payload.alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pending)
        if (payload.alarm) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setSound(NotificationChannels.alarmSound(this))
                    .setVibrate(NotificationChannels.alarmVibration)
            }
            val fullScreenAllowed = Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()
            if (fullScreenAllowed) {
                val alarmIntent = Intent(this, MilestoneAlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = android.net.Uri.parse("soundisco://alarm/${payload.notificationId}")
                    putExtra("notification_id", payload.notificationId)
                    putExtra("recipient_id", payload.recipientId)
                    putExtra("title", payload.title)
                    putExtra("body", payload.body)
                }
                val alarmPending = PendingIntent.getActivity(this, 0, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                builder.setFullScreenIntent(alarmPending, true)
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        }
        manager.notify(payload.notificationId, 0, builder.build())
    }

    companion object { const val CHANNEL = NotificationChannels.OPERATIONS }
}
