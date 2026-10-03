package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.model.AssignmentDetails
import edu.ucne.soundsicoappandroid.domain.model.Milestone
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
    suspend operator fun invoke(details: AssignmentDetails, milestone: Milestone, userId: String): AssignmentDetails {
        val personal = details.assignment.milestones.filter { it.isAssignedTo(userId) }.sortedBy { it.order }
        val index = personal.indexOfFirst { it.id == milestone.id }
        require(index >= 0) { "No estás asignado a este hito." }
        require(!milestone.isConfirmedBy(userId) && details.checkIns.none { it.milestoneId == milestone.id && it.userId == userId }) {
            "Este hito ya fue confirmado."
        }
        require(personal.take(index).all { previous ->
            previous.isConfirmedBy(userId) || details.checkIns.any { it.milestoneId == previous.id && it.userId == userId }
        }) { "Confirma primero tu hito anterior." }
        repository.checkIn(milestone.id, userId)
        return repository.getDetails(details.assignment.id, userId, false)
    }
}
