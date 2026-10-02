package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.Bulletin

interface BulletinsRepository {
    suspend fun getBulletins(): List<Bulletin>
    suspend fun getBulletin(id: String): Bulletin
    suspend fun createBulletin(subject: String, message: String): Bulletin
    suspend fun updateBulletin(id: String, subject: String, message: String): Bulletin
    suspend fun deleteBulletin(id: String)
}
