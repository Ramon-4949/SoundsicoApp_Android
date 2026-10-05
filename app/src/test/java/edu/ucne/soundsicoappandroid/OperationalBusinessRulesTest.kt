package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.Assignment
import edu.ucne.soundsicoappandroid.domain.model.AssignmentCollaborator
import edu.ucne.soundsicoappandroid.domain.model.AssignmentDetails
import edu.ucne.soundsicoappandroid.domain.model.AssignmentNote
import edu.ucne.soundsicoappandroid.domain.model.Milestone
import edu.ucne.soundsicoappandroid.domain.model.MilestoneCheckIn
import edu.ucne.soundsicoappandroid.domain.model.MilestoneCollaborator
import edu.ucne.soundsicoappandroid.domain.repository.MilestonesRepository
import edu.ucne.soundsicoappandroid.domain.usecase.ConfirmMilestoneUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.ObserveAssignmentDetailsUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.UndoMilestoneConfirmationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalBusinessRulesTest {
    @Test
    fun milestoneCompletesOnlyWhenEveryCollaboratorConfirmed() {
        val pending = milestone(false, false)
        val partiallyConfirmed = milestone(true, false)
        val staleGlobalState = partiallyConfirmed.copy(completed = true)
        val completed = milestone(true, true)

        assertFalse(pending.globallyCompleted)
        assertFalse(partiallyConfirmed.globallyCompleted)
        assertFalse(staleGlobalState.globallyCompleted)
        assertTrue(completed.globallyCompleted)
    }

    @Test
    fun employeeCannotObserveAssignmentWithoutAssignedMilestones() = runTest {
        val repository = RuleMilestonesRepository(details(milestone(false, false)))
        val useCase = ObserveAssignmentDetailsUseCase(repository)
        var denied = false

        try {
            useCase("assignment", "outsider", false).first()
        } catch (_: IllegalArgumentException) {
            denied = true
        }

        assertTrue(denied)
    }

    @Test
    fun confirmationHasNoTimeToleranceAndDoesNotCompleteOtherCollaborators() = runTest {
        val repository = RuleMilestonesRepository(details(milestone(false, false)))
        val useCase = ConfirmMilestoneUseCase(repository)
        val initial = repository.current.value

        val result = useCase(initial, initial.assignment.milestones.single(), "employee-a")

        assertTrue(result.assignment.milestones.single().isConfirmedBy("employee-a"))
        assertFalse(result.assignment.milestones.single().globallyCompleted)
    }

    @Test
    fun employeeCanUndoAndConfirmAnOverdueMilestoneAgain() = runTest {
        val repository = RuleMilestonesRepository(details(milestone(false, false)))
        val confirm = ConfirmMilestoneUseCase(repository)
        val undo = UndoMilestoneConfirmationUseCase(repository)
        val milestone = repository.current.value.assignment.milestones.single()

        val confirmed = confirm(repository.current.value, milestone, "employee-a")
        val pending = undo(confirmed, confirmed.assignment.milestones.single(), "employee-a")

        assertFalse(pending.assignment.milestones.single().isConfirmedBy("employee-a"))
        val confirmedAgain = confirm(pending, pending.assignment.milestones.single(), "employee-a")
        assertTrue(confirmedAgain.assignment.milestones.single().isConfirmedBy("employee-a"))
    }

    private fun milestone(firstConfirmed: Boolean, secondConfirmed: Boolean) = Milestone(
        id = "milestone",
        assignmentId = "assignment",
        order = 1,
        title = "Montaje de luces",
        scheduledAt = "2020-01-01T08:00:00-04:00",
        collaborators = listOf(
            MilestoneCollaborator("milestone", "employee-a", "asignado", "2020-01-01T08:05:00-04:00".takeIf { firstConfirmed }),
            MilestoneCollaborator("milestone", "employee-b", "asignado", "2020-01-01T08:10:00-04:00".takeIf { secondConfirmed })
        )
    )

    private fun details(milestone: Milestone) = AssignmentDetails(
        Assignment("assignment", "Evento", "campo", "alta", "en_curso", "Auditorio", null, null, milestones = listOf(milestone)),
        emptyList(),
        emptyList(),
        emptyList()
    )

    private class RuleMilestonesRepository(initial: AssignmentDetails) : MilestonesRepository {
        val current = MutableStateFlow(initial)

        override suspend fun getDetails(assignmentId: String, userId: String, administrator: Boolean) = current.value
        override suspend fun getCheckIns(assignmentId: String, userId: String) = current.value.checkIns
        override suspend fun getNotes(assignmentId: String) = emptyList<AssignmentNote>()
        override suspend fun getCollaborators(assignmentId: String) = emptyList<AssignmentCollaborator>()
        override suspend fun checkIn(milestoneId: String, userId: String): MilestoneCheckIn {
            val checkIn = MilestoneCheckIn("check", milestoneId, userId, "2026-10-03T12:00:00-04:00", "a_tiempo")
            val updatedMilestones = current.value.assignment.milestones.map { milestone ->
                milestone.copy(collaborators = milestone.collaborators.map { collaborator ->
                    if (collaborator.userId == userId) collaborator.copy(confirmedAt = checkIn.createdAt) else collaborator
                })
            }
            current.value = current.value.copy(
                assignment = current.value.assignment.copy(milestones = updatedMilestones),
                checkIns = current.value.checkIns + checkIn
            )
            return checkIn
        }
        override suspend fun undoCheckIn(milestoneId: String) {
            val userId = current.value.checkIns.firstOrNull { it.milestoneId == milestoneId }?.userId ?: return
            current.value = current.value.copy(
                assignment = current.value.assignment.copy(milestones = current.value.assignment.milestones.map { milestone ->
                    if (milestone.id != milestoneId) milestone else milestone.copy(
                        collaborators = milestone.collaborators.map { collaborator ->
                            if (collaborator.userId == userId) collaborator.copy(confirmedAt = null) else collaborator
                        }
                    )
                }),
                checkIns = current.value.checkIns.filterNot { it.milestoneId == milestoneId && it.userId == userId }
            )
        }
        override suspend fun addNote(id: String, assignmentId: String, content: String) = Unit
        override fun observeDetails(assignmentId: String, userId: String, administrator: Boolean): Flow<AssignmentDetails> = current
    }
}
