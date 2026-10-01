package edu.ucne.soundsicoappandroid.data.local

import android.content.Context
import edu.ucne.soundsicoappandroid.domain.repository.LoginPreferences

class AndroidLoginPreferences(context: Context) : LoginPreferences {
    private val preferences = context.getSharedPreferences("login_preferences", Context.MODE_PRIVATE)
    override fun rememberedEmail(): String = preferences.getString("email", "").orEmpty()
    override fun saveEmail(email: String?) {
        preferences.edit().apply {
            if (email == null) remove("email") else putString("email", email)
        }.apply()
    }
}
