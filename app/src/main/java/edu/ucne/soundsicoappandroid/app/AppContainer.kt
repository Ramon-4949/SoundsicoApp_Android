package edu.ucne.soundsicoappandroid.app

import android.content.Context
import edu.ucne.soundsicoappandroid.core.network.SupabaseProvider
import edu.ucne.soundsicoappandroid.data.local.AndroidLoginPreferences
import edu.ucne.soundsicoappandroid.data.repository.*
import edu.ucne.soundsicoappandroid.domain.usecase.*

class AppContainer(context: Context) {
    private val authRepository = SupabaseAuthRepository(SupabaseProvider.client)
    private val homeRepository = SupabaseHomeRepository(SupabaseProvider.client)
    val loginPreferences = AndroidLoginPreferences(context)
    val login = LoginUseCase(authRepository)
    val signUp = SignUpUseCase(authRepository)
    val observeSession = ObserveSessionUseCase(authRepository)
    val getProfile = GetProfileUseCase(authRepository)
    val getHomeContent = GetHomeContentUseCase(homeRepository)
    val signOut = SignOutUseCase(authRepository)
}
