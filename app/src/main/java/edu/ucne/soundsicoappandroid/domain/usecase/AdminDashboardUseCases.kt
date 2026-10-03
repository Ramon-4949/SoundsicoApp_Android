package edu.ucne.soundsicoappandroid.domain.usecase

import edu.ucne.soundsicoappandroid.domain.model.AccountAccess
import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile
import edu.ucne.soundsicoappandroid.domain.repository.AdminRepository
import kotlinx.coroutines.flow.map

class ObserveAdminDashboardUseCase(private val home: GetHomeContentUseCase) {
    operator fun invoke(profile: EmployeeProfile) = home.observe(profile).map {
        requireNotNull(it.dashboard) { "No se recibió el panel de administración." }
    }
}

class ReviewPendingAccountUseCase(private val home: GetHomeContentUseCase) {
    suspend operator fun invoke(profile: EmployeeProfile, userId: String, approved: Boolean) =
        home.reviewAccount(profile, userId, approved)
}

class GetEmployeePerformanceUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(profile: EmployeeProfile, month: String) =
        profile.requireAdministrator().let { repository.getEmployeePerformance(java.time.YearMonth.parse(month.take(7)).atDay(1).toString()) }
}

private fun EmployeeProfile.requireAdministrator() {
    require(access == AccountAccess.Approved && isAdministrator) {
        "Esta operación requiere una cuenta de administrador autorizada."
    }
}

class ManageAccountsUseCase(private val repository: AdminRepository) {
    suspend fun load(profile: EmployeeProfile) = profile.requireAdministrator().let { repository.getAccounts() }
    suspend fun review(profile: EmployeeProfile, id: String, state: edu.ucne.soundsicoappandroid.domain.model.AccountAccessState) {
        profile.requireAdministrator()
        repository.reviewAccount(id, state)
    }
}
