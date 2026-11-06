package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.model.AccountAccess
import edu.ucne.soundsicoappandroid.domain.model.AssignmentDay
import edu.ucne.soundsicoappandroid.domain.model.AssignmentFeedFilter
import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile
import edu.ucne.soundsicoappandroid.domain.repository.HomeRepository

class GetHomeContentUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke(profile: EmployeeProfile) = repository.load(profile.approvedId(), profile.isAdministrator)

    fun observe(profile: EmployeeProfile) = repository.observe(profile.approvedId(), profile.isAdministrator)

    suspend fun assignmentPage(
        profile: EmployeeProfile,
        offset: Int,
        filter: AssignmentFeedFilter,
        search: String = "",
        day: AssignmentDay? = null
    ) = repository.loadAssignmentPage(profile.approvedId(), profile.isAdministrator, offset, filter, search.trim().take(100), day)

    suspend fun reviewAccount(profile: EmployeeProfile, userId: String, approved: Boolean) {
        profile.requireAdministrator()
        repository.reviewAccount(userId, approved)
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
