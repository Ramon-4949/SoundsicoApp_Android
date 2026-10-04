package edu.ucne.soundsicoappandroid.app

import android.app.Application

class SoundiscoApplication : Application() {
    val container by lazy { AppContainer(applicationContext) }
    override fun onCreate() {
        super.onCreate()
        edu.ucne.soundsicoappandroid.core.notifications.NotificationChannels.create(this)
    }
}
