package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<SessionState>
    suspend fun login(email: String, password: String)
    suspend fun signUp(registration: Registration)
    suspend fun requestPasswordRecovery(email: String)
    suspend fun verifyPasswordRecovery(email: String, code: String)
    suspend fun updateRecoveredPassword(password: String)
    suspend fun cancelPasswordRecovery()
    suspend fun changePassword(currentPassword: String, newPassword: String)
    suspend fun signOut()
    suspend fun deleteAccount()
    suspend fun profile(userId: String): EmployeeProfile
}
