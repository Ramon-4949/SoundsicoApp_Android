package edu.ucne.soundsicoappandroid.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import edu.ucne.soundsicoappandroid.R

object NotificationChannels {
    const val OPERATIONS = "soundisco_notifications"
    const val ALARMS = "soundisco_milestone_alarms"

    val alarmVibration: LongArray get() = longArrayOf(0, 800, 200, 800, 200, 1_200)

    fun alarmSound(context: Context): Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.milestone_alarm}")

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(OPERATIONS, "Operaciones SounDisco", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Asignaciones, hitos y comunicados de SounDisco"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
        val alarmChannel = NotificationChannel(ALARMS, "Alarmas de hitos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarmas de hitos críticos y vencidos"
            enableVibration(true)
            vibrationPattern = alarmVibration
            setSound(alarmSound(context), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, alarmChannel))
    }
}
