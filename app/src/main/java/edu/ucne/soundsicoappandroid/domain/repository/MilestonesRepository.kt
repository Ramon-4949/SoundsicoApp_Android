package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.AssignmentCollaborator
import edu.ucne.soundsicoappandroid.domain.model.AssignmentDetails
import edu.ucne.soundsicoappandroid.domain.model.AssignmentNote
import edu.ucne.soundsicoappandroid.domain.model.MilestoneCheckIn
import kotlinx.coroutines.flow.Flow

interface MilestonesRepository {
    suspend fun getDetails(assignmentId: String, userId: String, administrator: Boolean = false): AssignmentDetails
    suspend fun getCheckIns(assignmentId: String, userId: String): List<MilestoneCheckIn>
    suspend fun getNotes(assignmentId: String): List<AssignmentNote>
    suspend fun getCollaborators(assignmentId: String): List<AssignmentCollaborator>
    suspend fun checkIn(milestoneId: String, userId: String): MilestoneCheckIn
    suspend fun addNote(id: String, assignmentId: String, content: String)
    fun observeDetails(assignmentId: String, userId: String, administrator: Boolean = false): Flow<AssignmentDetails>
}
