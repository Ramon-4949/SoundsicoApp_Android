package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile

interface ProfileRepository {
    suspend fun getProfile(userId: String): EmployeeProfile
    suspend fun updateProfile(username: String, phone: String, position: String): EmployeeProfile
}
