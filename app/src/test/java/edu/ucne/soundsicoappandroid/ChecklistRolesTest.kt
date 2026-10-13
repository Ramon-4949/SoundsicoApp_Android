package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.canOversee
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.deliveryLabel
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.isConfirmed
import org.junit.Assert.*
import org.junit.Test

class ChecklistRolesTest {
    private val milestone = Milestone("hito", collaborators = listOf(
        MilestoneCollaborator("hito", "a", "temprano", "2026-10-03T10:45:00-04:00", "2026-10-03T11:00:00-04:00"),
        MilestoneCollaborator("hito", "b", "pendiente")
    ))
    private val details = AssignmentDetails(
        Assignment("evento", "Montaje", "campo", "alta", "en_curso", null, null, null,
            milestones = listOf(milestone), supervisors = listOf(AssignmentSupervisor("supervisor"))),
        emptyList(), emptyList(), emptyList()
    )

    @Test fun onlyAdministratorAndAssignedSupervisorOverseeTeam() {
        assertTrue(details.canOversee("admin", true))
        assertTrue(details.canOversee("supervisor", false))
        assertFalse(details.canOversee("a", false))
        assertFalse(details.canOversee("another-supervisor", false))
    }

    @Test fun personalConfirmationDoesNotCompleteTeamMilestone() {
        assertTrue(milestone.isConfirmed(details, "a", false))
        assertFalse(milestone.isConfirmed(details, "b", false))
        assertFalse(milestone.isConfirmed(details, "supervisor", true))
    }

    @Test fun deliveryLabelsUseIndividualEvaluationAndTimestamps() {
        assertEquals("Temprano (-15 min)", milestone.collaborators.first().deliveryLabel())
        assertEquals("Sin confirmar", milestone.collaborators.last().deliveryLabel())
        assertEquals("Tardío (+12 min)", milestone.collaborators.first().copy(
            status = "tardio", confirmedAt = "2026-10-03T11:12:00-04:00").deliveryLabel())
    }

    @Test fun serverEvaluationRemainsAuthoritativeAcrossTheFifteenMinuteBoundary() {
        val member = milestone.collaborators.first().copy(scheduledAt = "2026-10-03T15:00:00Z")
        assertEquals("A tiempo", member.copy(status = "a_tiempo", confirmedAt = "2026-10-03T11:15:00-04:00").deliveryLabel())
        assertEquals("Tardío (+15 min)", member.copy(status = "tardio", confirmedAt = "2026-10-03T11:15:01-04:00").deliveryLabel())
        assertEquals("Temprano (-15 min)", member.copy(status = "temprano", confirmedAt = "2026-10-03T10:45:00-04:00").deliveryLabel())
        assertEquals("A tiempo", member.copy(status = "a_tiempo", confirmedAt = "2026-10-03T16:00:00Z").deliveryLabel())
    }

    @Test fun assignedAdminFollowsTheirOwnSequenceWhileTheTeamIsStillPending() {
        val first = Milestone("first", order = 1, collaborators = listOf(
            MilestoneCollaborator("first", "admin", "sin_confirmar"),
            MilestoneCollaborator("first", "employee", "sin_confirmar")
        ))
        val second = Milestone("second", order = 2, scheduledAt = "2020-01-01T00:00:00Z",
            collaborators = listOf(MilestoneCollaborator("second", "admin", "sin_confirmar")))
        val pending = details.copy(assignment = details.assignment.copy(milestones = listOf(first, second)))
        assertNull(pending.confirmationBlock("first", "admin"))
        assertEquals("Confirma primero tu hito anterior.", pending.confirmationBlock("second", "admin"))
        val partial = first.copy(collaborators = first.collaborators.map {
            if (it.userId == "admin") it.copy(confirmedAt = "2026-10-12T12:00:00Z", status = "a_tiempo") else it
        })
        val confirmed = pending.copy(assignment = pending.assignment.copy(milestones = listOf(partial, second)))
        assertFalse(partial.globallyCompleted)
        assertNull(confirmed.confirmationBlock("second", "admin"))
        assertEquals("No estás asignado a este hito.", confirmed.confirmationBlock("second", "employee"))
        assertFalse(second.copy(collaborators = listOf(MilestoneCollaborator("wrong", "admin", "sin_confirmar"))).isAssignedTo("admin"))
    }
}
