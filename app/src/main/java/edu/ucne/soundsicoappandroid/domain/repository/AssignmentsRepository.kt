package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.Assignment
import kotlinx.coroutines.flow.Flow

interface AssignmentsRepository {
    suspend fun getAssignments(userId: String): List<Assignment>
    suspend fun getAssignment(id: String, viewerId: String? = null): Assignment
    fun observeAssignments(userId: String): Flow<List<Assignment>>
}
