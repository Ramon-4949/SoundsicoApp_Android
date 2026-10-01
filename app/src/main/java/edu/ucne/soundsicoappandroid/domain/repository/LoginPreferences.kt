package edu.ucne.soundsicoappandroid.domain.repository

interface LoginPreferences {
    fun rememberedEmail(): String
    fun saveEmail(email: String?)
}
