package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    suspend fun load(userId: String, administrator: Boolean): HomeContent
    suspend fun loadDashboard(): AdminDashboard
    suspend fun loadAssignmentPage(
        userId: String,
        administrator: Boolean,
        offset: Int,
        filter: AssignmentFeedFilter,
        search: String,
        day: AssignmentDay?
    ): AssignmentPage
    suspend fun reviewAccount(userId: String, approved: Boolean)
    fun observe(userId: String, administrator: Boolean): Flow<HomeContent>
}
