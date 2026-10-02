package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<SessionState>
    suspend fun login(email: String, password: String)
    suspend fun signUp(registration: Registration)
    suspend fun signOut()
    suspend fun deleteAccount()
    suspend fun profile(userId: String): EmployeeProfile
}
