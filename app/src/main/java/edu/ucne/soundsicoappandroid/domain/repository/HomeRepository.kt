package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.HomeContent

interface HomeRepository {
    suspend fun load(userId: String, administrator: Boolean): HomeContent
}
