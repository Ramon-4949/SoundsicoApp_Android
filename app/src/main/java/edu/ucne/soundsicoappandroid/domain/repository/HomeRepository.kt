package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.*

interface HomeRepository {
    suspend fun load(userId: String, administrator: Boolean): HomeContent
    suspend fun loadEmployeeAssignments(userId: String): List<Assignment>
    suspend fun loadDashboard(): AdminDashboard
    suspend fun loadAdminAssignments(offset: Int, filter: String, search: String): AssignmentPage
    suspend fun reviewAccount(userId: String, approved: Boolean)
}
