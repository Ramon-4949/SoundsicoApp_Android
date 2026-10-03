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
}
