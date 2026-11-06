package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.data.remote.AssignmentDto
import edu.ucne.soundsicoappandroid.data.remote.MilestoneDto
import edu.ucne.soundsicoappandroid.data.remote.CollaboratorDto
import edu.ucne.soundsicoappandroid.data.remote.SupervisorDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AssignmentMappingTest {
    private val now = Instant.parse("2026-10-02T16:00:00Z")

    @Test
    fun adminAssignmentUsesEffectiveOverdueStatus() {
        val assignment = AssignmentDto(
            id = "assignment",
            title = "Reunión de sistema oficina",
            status = "en_curso",
            deadline = "2026-10-01T13:00:00-04:00"
        ).toDomain("admin", true, now)

        assertEquals("vencida", assignment.status)
    }

    @Test
    fun completedStatusWinsAfterDeadline() {
        val assignment = AssignmentDto(
            id = "assignment",
            title = "Prueba",
            status = "completada",
            deadline = "2026-09-24T22:40:00-04:00"
        ).toDomain("admin", true, now)

        assertEquals("completada", assignment.status)
    }

    @Test
    fun lastMilestoneActsAsDeadlineWhenAssignmentHasNone() {
        val assignment = AssignmentDto(
            id = "assignment",
            title = "PLD",
            status = "en_curso",
            milestones = listOf(
                MilestoneDto(id = "milestone", order = 1, date = "2026-09-28T02:25:00-04:00")
            )
        ).toDomain("admin", true, now)

        assertEquals("vencida", assignment.status)
    }

    @Test
    fun employeeAssignmentWithoutPersonalMilestonesKeepsOverdueStatus() {
        val assignment = AssignmentDto(
            id = "assignment",
            title = "Evento vencido",
            status = "en_curso",
            deadline = "2026-10-01T13:00:00-04:00",
            milestones = listOf(
                MilestoneDto(id = "milestone", order = 1, date = "2026-10-01T13:00:00-04:00")
            )
        ).toDomain("employee", false, now)

        assertEquals("vencida", assignment.status)
    }

    @Test
    fun personalCompletionAndSupervisorStatusRemainSeparate() {
        val dto = AssignmentDto(
            id = "assignment", title = "Evento", status = "vencida",
            deadline = "2026-10-02T14:00:00Z",
            milestones = listOf(
                MilestoneDto(id = "first", order = 1, date = "2026-10-02T14:00:00Z", collaborators = listOf(
                    CollaboratorDto(userId = "finished", confirmedAt = "2026-10-02T14:00:00Z")
                )),
                MilestoneDto(id = "second", order = 2, date = "2026-10-03T14:00:00Z", collaborators = listOf(
                    CollaboratorDto(userId = "pending"), CollaboratorDto(userId = "supervisor")
                ))
            ),
            supervisors = listOf(SupervisorDto("supervisor"))
        )
        assertEquals("completada", dto.toDomain("finished", false, now).status)
        assertEquals("pendiente", dto.toDomain("pending", false, now).status)
        assertEquals("vencida", dto.toDomain("supervisor", false, now).status)
    }
}
