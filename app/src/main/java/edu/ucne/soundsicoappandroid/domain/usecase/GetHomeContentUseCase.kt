package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.repository.HomeRepository

class GetHomeContentUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke(userId: String, administrator: Boolean) = repository.load(userId, administrator)
    suspend fun adminAssignments(offset: Int, filter: String, search: String) =
        repository.loadAdminAssignments(offset, filter, search.trim().take(100))
    suspend fun reviewAccount(userId: String, approved: Boolean) = repository.reviewAccount(userId, approved)
    suspend fun adminCalendar(): List<edu.ucne.soundsicoappandroid.domain.model.Assignment> {
        val result = mutableListOf<edu.ucne.soundsicoappandroid.domain.model.Assignment>()
        var offset = 0
        do {
            val page = repository.loadAdminAssignments(offset, "todas", "")
            result.addAll(page.items)
            val next = page.nextOffset
            check(next == null || next > offset) { "El servidor devolvió una página inválida." }
            offset = next ?: break
        } while (true)
        return result.distinctBy { it.id }
    }
}
