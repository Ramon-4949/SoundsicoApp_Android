package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.CalendarAgenda

interface CalendarRepository {
    suspend fun getAgenda(userId: String, date: String): CalendarAgenda
}
