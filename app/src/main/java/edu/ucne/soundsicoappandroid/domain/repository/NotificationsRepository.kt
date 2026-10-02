package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.EmployeeNotification
import kotlinx.coroutines.flow.Flow

interface NotificationsRepository {
    suspend fun getNotifications(userId: String): List<EmployeeNotification>
    suspend fun markRead(id: String? = null)
    fun observeNotifications(userId: String): Flow<List<EmployeeNotification>>
}
