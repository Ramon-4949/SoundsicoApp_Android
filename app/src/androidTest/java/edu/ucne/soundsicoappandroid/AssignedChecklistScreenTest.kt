package edu.ucne.soundsicoappandroid

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import edu.ucne.soundsicoappandroid.core.designsystem.SoundiscoTheme
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AssignedChecklistScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun details(assigned: Boolean, confirmed: Boolean = false): AssignmentDetails {
        val members = listOf(
            MilestoneCollaborator("hito", "employee", "sin_confirmar", employee = Employee("employee", "Técnico del equipo", "empleado", "Audio"))
        ) + if (assigned) listOf(
            MilestoneCollaborator("hito", "admin", if (confirmed) "a_tiempo" else "sin_confirmar",
                confirmedAt = "2026-10-12T12:15:00-04:00".takeIf { confirmed },
                scheduledAt = "2026-10-12T16:00:00Z", employee = Employee("admin", "Admin asignado", "admin", "Gestión"))
        ) else emptyList()
        val milestone = Milestone("hito", "assignment", 1, "Montaje", scheduledAt = "2026-10-12T16:00:00Z", collaborators = members)
        return AssignmentDetails(
            Assignment("assignment", "Evento", "campo", "alta", "en_curso", null, null, null, milestones = listOf(milestone)),
            emptyList(), emptyList(), emptyList()
        )
    }

    @Test
    fun assignedAdminSeesTeamAndCanConfirmTheirMilestone() {
        val details = details(true)
        val intents = mutableListOf<AssignmentDetailIntent>()
        compose.setContent {
            SoundiscoTheme { AssignmentChecklistScreen(details, AssignmentDetailState(details = details), "admin", true, intents::add, {}) }
        }
        compose.onNodeWithText("Técnico del equipo").assertIsDisplayed()
        compose.onNodeWithTag("confirm-hito").assertIsEnabled().performClick()
        assertEquals("hito", (intents.single() as AssignmentDetailIntent.CheckIn).milestone.id)
    }

    @Test
    fun unassignedAdminSeesTeamWithoutConfirmationActions() {
        val details = details(false)
        compose.setContent {
            SoundiscoTheme { AssignmentChecklistScreen(details, AssignmentDetailState(details = details), "admin", true, {}, {}) }
        }
        compose.onNodeWithText("Técnico del equipo").assertIsDisplayed()
        compose.onNodeWithTag("confirm-hito").assertDoesNotExist()
        compose.onNodeWithTag("undo-hito").assertDoesNotExist()
    }

    @Test
    fun assignedAdminCanUndoTheirDeliveryWhileOtherTechniciansRemainPending() {
        val details = details(true, true)
        val intents = mutableListOf<AssignmentDetailIntent>()
        compose.setContent {
            SoundiscoTheme { AssignmentChecklistScreen(details, AssignmentDetailState(details = details), "admin", true, intents::add, {}) }
        }
        compose.onNodeWithText("A tiempo").assertIsDisplayed()
        compose.onNodeWithTag("undo-hito").assertIsEnabled().performClick()
        compose.onNodeWithText("Deshacer", useUnmergedTree = true).performClick()
        assertEquals("hito", (intents.single() as AssignmentDetailIntent.UndoCheckIn).milestone.id)
        assertFalse(details.assignment.milestones.single().globallyCompleted)
    }

    @Test
    fun assignedEmployeeKeepsPersonalConfirmation() {
        val details = details(true)
        val intents = mutableListOf<AssignmentDetailIntent>()
        compose.setContent {
            SoundiscoTheme { AssignmentChecklistScreen(details, AssignmentDetailState(details = details), "employee", false, intents::add, {}) }
        }
        compose.onNodeWithTag("confirm-hito").assertIsEnabled().performClick()
        compose.onNodeWithText("Admin asignado").assertDoesNotExist()
        assertTrue(intents.single() is AssignmentDetailIntent.CheckIn)
    }
}
