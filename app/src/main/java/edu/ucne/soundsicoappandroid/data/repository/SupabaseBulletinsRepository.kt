package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.BulletinCreateDto
import edu.ucne.soundsicoappandroid.data.remote.BulletinDto
import edu.ucne.soundsicoappandroid.data.remote.BulletinUpdateDto
import edu.ucne.soundsicoappandroid.domain.model.Bulletin
import edu.ucne.soundsicoappandroid.domain.repository.BulletinsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant

class SupabaseBulletinsRepository(private val client: SupabaseClient) : BulletinsRepository {
    override suspend fun getBulletins(): List<Bulletin> {
        val result = mutableListOf<BulletinDto>()
        var offset = 0L
        do {
            val page = client.from("comunicados").select {
                order("fecha_publicacion", Order.DESCENDING)
                range(offset, offset + 199)
            }.decodeList<BulletinDto>()
            result += page
            offset += 200
        } while (page.size == 200)
        return result.map { it.toDomain() }
    }

    override suspend fun getBulletin(id: String): Bulletin = client.from("comunicados").select {
        filter { eq("id", id) }
    }.decodeSingle<BulletinDto>().toDomain()

    override suspend fun createBulletin(subject: String, message: String): Bulletin =
        client.from("comunicados").insert(BulletinCreateDto(subject.trim(), message.trim(), Instant.now().toString())) {
            select()
        }.decodeSingle<BulletinDto>().toDomain()

    override suspend fun updateBulletin(id: String, subject: String, message: String): Bulletin =
        client.from("comunicados").update(BulletinUpdateDto(subject.trim(), message.trim())) {
            filter { eq("id", id) }
            select()
        }.decodeSingle<BulletinDto>().toDomain()

    override suspend fun deleteBulletin(id: String) {
        client.from("comunicados").delete { filter { eq("id", id) } }
    }
}
