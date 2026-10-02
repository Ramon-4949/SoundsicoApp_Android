package edu.ucne.soundsicoappandroid.domain.repository

interface LoginPreferences {
    fun rememberedEmail(): String
    fun saveEmail(email: String?)
    fun biometricEnabled(userId: String): Boolean
    fun setBiometricEnabled(userId: String, enabled: Boolean)
}
