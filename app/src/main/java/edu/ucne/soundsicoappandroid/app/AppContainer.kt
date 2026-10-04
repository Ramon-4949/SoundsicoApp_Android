package edu.ucne.soundsicoappandroid.app

import android.content.Context
import edu.ucne.soundsicoappandroid.core.network.SupabaseProvider
import edu.ucne.soundsicoappandroid.data.local.AndroidLoginPreferences
import edu.ucne.soundsicoappandroid.data.repository.*
import edu.ucne.soundsicoappandroid.domain.usecase.*

class AppContainer(context: Context) {
    val pushRegistration = edu.ucne.soundsicoappandroid.core.notifications.PushRegistration(context)
    private val authRepository = SupabaseAuthRepository(SupabaseProvider.client, pushRegistration)
    private val homeRepository = SupabaseHomeRepository(SupabaseProvider.client)
    val assignmentsRepository = SupabaseAssignmentsRepository(SupabaseProvider.client)
    val milestonesRepository = SupabaseMilestonesRepository(SupabaseProvider.client)
    val profileRepository = SupabaseProfileRepository(SupabaseProvider.client)
    val calendarRepository = SupabaseCalendarRepository(assignmentsRepository)
    val bulletinsRepository = SupabaseBulletinsRepository(SupabaseProvider.client)
    val adminRepository = SupabaseAdminRepository(SupabaseProvider.client)
    val notificationsRepository = SupabaseNotificationsRepository(SupabaseProvider.client)
    val loginPreferences = AndroidLoginPreferences(context)
    val login = LoginUseCase(authRepository)
    val signUp = SignUpUseCase(authRepository)
    val observeSession = ObserveSessionUseCase(authRepository)
    val requestPasswordRecovery = RequestPasswordRecoveryUseCase(authRepository)
    val verifyPasswordRecovery = VerifyPasswordRecoveryUseCase(authRepository)
    val updateRecoveredPassword = UpdateRecoveredPasswordUseCase(authRepository)
    val cancelPasswordRecovery = CancelPasswordRecoveryUseCase(authRepository)
    val changePassword = ChangePasswordUseCase(authRepository)
    val getProfile = GetProfileUseCase(authRepository)
    val getHomeContent = GetHomeContentUseCase(homeRepository)
    val observeAdminDashboard = ObserveAdminDashboardUseCase(getHomeContent)
    val reviewPendingAccount = ReviewPendingAccountUseCase(getHomeContent)
    val manageAccounts = ManageAccountsUseCase(adminRepository)
    val getEmployeePerformance = GetEmployeePerformanceUseCase(adminRepository)
    val signOut = SignOutUseCase(authRepository)
    val deleteAccount = DeleteAccountUseCase(authRepository)
    val observeAssignmentDetails = ObserveAssignmentDetailsUseCase(milestonesRepository)
    val confirmMilestone = ConfirmMilestoneUseCase(milestonesRepository)
}
