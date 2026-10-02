package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.domain.model.CalendarAgenda
import edu.ucne.soundsicoappandroid.domain.repository.AssignmentsRepository
import edu.ucne.soundsicoappandroid.domain.repository.CalendarRepository
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class SupabaseCalendarRepository(private val assignments: AssignmentsRepository) : CalendarRepository {
    override suspend fun getAgenda(userId: String, date: String): CalendarAgenda {
        val selected = LocalDate.parse(date)
        val entries = assignments.getAssignments(userId).filter { assignment ->
            assignment.deadline.toLocalDate() == selected || assignment.milestones.any {
                (it.scheduledAt ?: it.estimatedTime).toLocalDate() == selected
            }
        }
        return CalendarAgenda(date, entries)
    }

    private fun String?.toLocalDate(): LocalDate? {
        if (this == null) return null
        return runCatching { OffsetDateTime.parse(this).atZoneSameInstant(ZONE).toLocalDate() }
            .recoverCatching { Instant.parse(this).atZone(ZONE).toLocalDate() }
            .recoverCatching { LocalDate.parse(take(10)) }
            .getOrNull()
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("America/Santo_Domingo")
    }
}
