package edu.ucne.soundsicoappandroid.app

import android.app.Application

class SoundiscoApplication : Application() {
    val container by lazy { AppContainer(applicationContext) }
}
