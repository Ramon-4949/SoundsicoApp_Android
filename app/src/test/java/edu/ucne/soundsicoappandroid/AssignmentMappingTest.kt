package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.data.remote.AssignmentDto
import edu.ucne.soundsicoappandroid.data.remote.MilestoneDto
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
}
