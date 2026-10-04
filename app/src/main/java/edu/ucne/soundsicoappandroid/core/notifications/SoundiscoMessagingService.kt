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
import java.util.UUID

class SoundiscoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushRegistration.enqueue(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val recipient = message.data["recipient_id"] ?: return
        val id = message.data["notification_id"] ?: return
        if (runCatching { UUID.fromString(id) }.isFailure) return
        val owner = getSharedPreferences("soundisco_push", MODE_PRIVATE).getString("owner", null)
        if (owner != recipient) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(NotificationManager::class.java)
        NotificationChannels.create(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = android.net.Uri.parse("soundisco://notification/$id")
            putExtra("notification_id", id)
            putExtra("recipient_id", recipient)
        }
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.data["title"] ?: "SounDisco")
            .setContentText(message.data["body"])
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.data["body"]))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(id, 0, notification)
    }

    companion object { const val CHANNEL = NotificationChannels.OPERATIONS }
}
