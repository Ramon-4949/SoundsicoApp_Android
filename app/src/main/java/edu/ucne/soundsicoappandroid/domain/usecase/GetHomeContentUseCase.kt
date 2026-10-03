package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.model.AccountAccess
import edu.ucne.soundsicoappandroid.domain.model.Assignment
import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile
import edu.ucne.soundsicoappandroid.domain.repository.HomeRepository

class GetHomeContentUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke(profile: EmployeeProfile) = repository.load(profile.approvedId(), profile.isAdministrator)

    fun observe(profile: EmployeeProfile) = repository.observe(profile.approvedId(), profile.isAdministrator)

    suspend fun adminAssignments(profile: EmployeeProfile, offset: Int, filter: String, search: String) =
        profile.requireAdministrator().let {
        repository.loadAdminAssignments(offset, filter, search.trim().take(100))
        }

    suspend fun reviewAccount(profile: EmployeeProfile, userId: String, approved: Boolean) {
        profile.requireAdministrator()
        repository.reviewAccount(userId, approved)
    }

    suspend fun adminCalendar(profile: EmployeeProfile): List<Assignment> {
        profile.requireAdministrator()
        val result = mutableListOf<Assignment>()
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

    private fun EmployeeProfile.approvedId(): String {
        require(access == AccountAccess.Approved) { "La cuenta no está autorizada." }
        return id
    }

    private fun EmployeeProfile.requireAdministrator() {
        approvedId()
        require(isAdministrator) { "Esta operación requiere permisos de administrador." }
    }
}
