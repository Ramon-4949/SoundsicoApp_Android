package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.model.AssignmentDetails
import edu.ucne.soundsicoappandroid.domain.model.Milestone
import edu.ucne.soundsicoappandroid.domain.model.confirmationBlock
import edu.ucne.soundsicoappandroid.domain.repository.MilestonesRepository
import kotlinx.coroutines.flow.map

class ObserveAssignmentDetailsUseCase(private val repository: MilestonesRepository) {
    operator fun invoke(assignmentId: String, userId: String, administrator: Boolean) =
        repository.observeDetails(assignmentId, userId, administrator).map { details ->
            require(administrator || details.assignment.supervisors.any { it.userId == userId } ||
                details.collaborators.any { it.id == userId && it.supervisor } || details.assignment.team.any { it.id == userId } ||
                details.assignment.milestones.any { it.isAssignedTo(userId) }) {
                "No tienes acceso a esta asignación."
            }
            details
        }
}

class ConfirmMilestoneUseCase(private val repository: MilestonesRepository) {
    suspend operator fun invoke(details: AssignmentDetails, milestone: Milestone, userId: String, administrator: Boolean = false): AssignmentDetails {
        val block = details.confirmationBlock(milestone.id, userId)
        require(block == null) { block.orEmpty() }
        repository.checkIn(milestone.id, userId)
        return repository.getDetails(details.assignment.id, userId, administrator)
    }
}

class UndoMilestoneConfirmationUseCase(private val repository: MilestonesRepository) {
    suspend operator fun invoke(details: AssignmentDetails, milestone: Milestone, userId: String, administrator: Boolean = false): AssignmentDetails {
        val current = requireNotNull(details.assignment.milestones.firstOrNull { it.id == milestone.id }) {
            "No estás asignado a este hito."
        }
        require(current.isAssignedTo(userId)) { "No estás asignado a este hito." }
        require(current.isConfirmedBy(userId) || details.checkIns.any {
            it.milestoneId == milestone.id && it.userId == userId
        }) { "Este hito todavía no ha sido confirmado." }
        repository.undoCheckIn(milestone.id)
        return repository.getDetails(details.assignment.id, userId, administrator)
    }
}
