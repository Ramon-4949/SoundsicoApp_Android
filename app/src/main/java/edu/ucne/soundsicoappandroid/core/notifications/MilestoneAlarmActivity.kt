package edu.ucne.soundsicoappandroid.core.notifications

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.app.MainActivity
import edu.ucne.soundsicoappandroid.core.designsystem.SoundiscoTheme

class MilestoneAlarmActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val owner = getSharedPreferences("soundisco_push", MODE_PRIVATE).getString("owner", null)
        val payload = PushNotificationPayload.parse(
            listOf("notification_id", "recipient_id", "title", "body").mapNotNull { key ->
                intent.getStringExtra(key)?.let { key to it }
            }.toMap(), owner
        )
        if (payload == null) {
            finish()
            return
        }
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        enableEdgeToEdge()
        setContent {
            SoundiscoTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Alarm, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(24.dp))
                        Text(payload.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(payload.body, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(32.dp))
                        Button({
                            getSystemService(NotificationManager::class.java).cancel(payload.notificationId, 0)
                            startActivity(Intent(this@MilestoneAlarmActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("notification_id", payload.notificationId)
                                putExtra("recipient_id", payload.recipientId)
                            })
                            finish()
                        }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Revisar hito") }
                        TextButton({ finish() }) { Text("Cerrar aviso") }
                    }
                }
            }
        }
    }
}
