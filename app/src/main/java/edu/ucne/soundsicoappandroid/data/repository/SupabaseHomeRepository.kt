package edu.ucne.soundsicoappandroid.data.repository

import edu.ucne.soundsicoappandroid.data.remote.*
import edu.ucne.soundsicoappandroid.domain.model.HomeContent
import edu.ucne.soundsicoappandroid.domain.repository.HomeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SupabaseHomeRepository(private val client: SupabaseClient) : HomeRepository {
    override suspend fun load(userId: String, administrator: Boolean): HomeContent = coroutineScope {
        val assignments = async {
            val result = mutableListOf<AssignmentDto>()
            var offset = 0L
            var pageSize: Int
            val columns = "*,hitos_itinerario(orden,fecha_programada,hitos_colaboradores(usuario_id,confirmado)),asignacion_supervisores(usuario_id)"
            do {
                val page = if (administrator) {
                    client.from("asignaciones").select(Columns.raw(columns)) {
                        order("id", Order.ASCENDING)
                        range(offset, offset + 199)
                    }.decodeList<AssignmentDto>().also { pageSize = it.size }
                } else {
                    client.from("asignacion_equipo").select(Columns.raw("asignaciones($columns)")) {
                        filter { eq("perfil_id", userId) }
                        order("asignacion_id", Order.ASCENDING)
                        range(offset, offset + 199)
                    }.decodeList<AssignmentLinkDto>().also { pageSize = it.size }.mapNotNull { it.asignaciones }
                }
                result.addAll(page)
                offset += 200
            } while (pageSize == 200)
            result.distinctBy { it.id }.map { it.toDomain(userId, administrator) }.sortedBy { it.deadline }
        }
        val bulletins = async {
            val result = mutableListOf<BulletinDto>()
            var offset = 0L
            do {
                val page = client.from("comunicados").select {
                    order("id", Order.DESCENDING)
                    range(offset, offset + 199)
                }.decodeList<BulletinDto>()
                result.addAll(page)
                offset += 200
            } while (page.size == 200)
            result.map { it.toDomain() }
        }
        HomeContent(assignments.await(), bulletins.await())
    }
}
