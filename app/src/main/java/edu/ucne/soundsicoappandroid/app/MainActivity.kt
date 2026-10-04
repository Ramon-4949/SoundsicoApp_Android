package edu.ucne.soundsicoappandroid.app

import android.os.Bundle
import android.content.Intent
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import edu.ucne.soundsicoappandroid.presentation.notifications.*
import edu.ucne.soundsicoappandroid.core.notifications.PushRegistration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ucne.soundsicoappandroid.core.designsystem.*
import edu.ucne.soundsicoappandroid.domain.model.Assignment
import edu.ucne.soundsicoappandroid.domain.model.SessionState
import edu.ucne.soundsicoappandroid.presentation.home.*
import edu.ucne.soundsicoappandroid.presentation.admincreation.*
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.*
import edu.ucne.soundsicoappandroid.presentation.bulletindetail.*
import edu.ucne.soundsicoappandroid.presentation.login.*
import edu.ucne.soundsicoappandroid.presentation.signup.*
import edu.ucne.soundsicoappandroid.presentation.authentication.*
import edu.ucne.soundsicoappandroid.presentation.profile.*

class MainActivity : ComponentActivity() {
    private var pushRoute by mutableStateOf<Pair<String, String>?>(null)

    private fun readRoute(intent: Intent) {
        val id = intent.getStringExtra("notification_id") ?: return
        val recipient = intent.getStringExtra("recipient_id") ?: return
        if (runCatching { java.util.UUID.fromString(id); java.util.UUID.fromString(recipient) }.isSuccess) pushRoute = id to recipient
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readRoute(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readRoute(intent)
        enableEdgeToEdge()
        val container = (application as SoundiscoApplication).container
        setContent {
            SoundiscoTheme { SoundiscoApp(container, pushRoute) { pushRoute = null } }
        }
    }
}

private inline fun <reified T : ViewModel> factory(crossinline create: () -> T) = object : ViewModelProvider.Factory {
    override fun <V : ViewModel> create(modelClass: Class<V>): V = modelClass.cast(create()) as V
}

@Composable
private fun SoundiscoApp(container: AppContainer, pushRoute: Pair<String, String>?, onRouteHandled: () -> Unit) {
    val sessionModel: SessionViewModel = viewModel(factory = factory { SessionViewModel(container.observeSession) })
    val session by sessionModel.state.collectAsStateWithLifecycle()
    when (val current = session) {
        SessionState.Loading -> Surface(Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                BrandLogo()
                Spacer(Modifier.height(24.dp))
                Text("SounDisco", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
        }
        SessionState.SignedOut -> SessionScope("signed-out", sessionModel) {
            LaunchedEffect(Unit) { container.pushRegistration.clear() }
            var registration by rememberSaveable { mutableStateOf(false) }
            var recovery by rememberSaveable { mutableStateOf(false) }
            var recoverySession by rememberSaveable { mutableIntStateOf(0) }
            if (recovery) {
                val recoveryModel: PasswordRecoveryViewModel = viewModel(key = "password-recovery-$recoverySession", factory = factory {
                    PasswordRecoveryViewModel(
                        container.loginPreferences.rememberedEmail(),
                        container.requestPasswordRecovery,
                        container.verifyPasswordRecovery,
                        container.updateRecoveredPassword,
                        container.cancelPasswordRecovery
                    )
                })
                val recoveryState by recoveryModel.state.collectAsStateWithLifecycle()
                PasswordRecoveryScreen(recoveryState, recoveryModel::onIntent) { recovery = false }
            } else if (registration) {
                val model: SignUpViewModel = viewModel(factory = factory { SignUpViewModel(container.signUp) })
                val state by model.state.collectAsStateWithLifecycle()
                SignUpScreen(state, model::onIntent) { registration = false }
            } else {
                val model: LoginViewModel = viewModel(factory = factory { LoginViewModel(container.login, container.loginPreferences) })
                val state by model.state.collectAsStateWithLifecycle()
                LoginScreen(state, model::onIntent, { registration = true }) {
                    recoverySession++
                    recovery = true
                }
            }
        }
        is SessionState.SignedIn -> SessionScope(current.user.id, sessionModel) {
            val model: HomeViewModel = viewModel(factory = factory {
                HomeViewModel(
                    current.user,
                    container.getProfile,
                    container.getHomeContent,
                    container.signOut,
                    container.getEmployeePerformance,
                    container.deleteAccount,
                    container.loginPreferences,
                    container.manageAccounts
                )
            })
            val state by model.state.collectAsStateWithLifecycle()
            val notifications: NotificationsViewModel = viewModel(factory = factory {
                NotificationsViewModel(current.user.id, container.notificationsRepository, container.assignmentsRepository, container.bulletinsRepository)
            })
            val notificationState by notifications.state.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                PushRegistration.enqueue(context)
            }
            LaunchedEffect(current.user.id) {
                container.pushRegistration.connect(current.user.id)
                if (container.pushRegistration.configured && Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    val preferences = context.getSharedPreferences("soundisco_push", android.content.Context.MODE_PRIVATE)
                    if (!preferences.getBoolean("permission_requested", false)) {
                        preferences.edit().putBoolean("permission_requested", true).apply()
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            LaunchedEffect(pushRoute, state.profile) {
                pushRoute?.let { (id, recipient) ->
                    if (recipient != current.user.id) onRouteHandled()
                    else if (state.profile != null) {
                        if (state.profile?.access == edu.ucne.soundsicoappandroid.domain.model.AccountAccess.Approved) {
                            model.onIntent(HomeIntent.OpenNotifications)
                            notifications.open(id, state.audience == HomeAudience.Administrator)
                        } else model.onIntent(HomeIntent.Refresh)
                        onRouteHandled()
                    }
                }
            }
            LaunchedEffect(notificationState.assignment, notificationState.bulletin) {
                notificationState.assignment?.let { model.onIntent(HomeIntent.OpenAssignment(it)) }
                notificationState.bulletin?.let { model.onIntent(HomeIntent.OpenBulletin(it)) }
                notifications.clearDestination()
            }
            var creating by rememberSaveable { mutableStateOf(false) }
            var creationSession by rememberSaveable { mutableIntStateOf(0) }
            var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
            var editingProfile by rememberSaveable { mutableStateOf(false) }
            var editProfileSession by rememberSaveable { mutableIntStateOf(0) }
            var accountOpen by rememberSaveable { mutableStateOf(false) }
            var changingPassword by rememberSaveable { mutableStateOf(false) }
            var passwordSession by rememberSaveable { mutableIntStateOf(0) }
            val administrator = state.audience == HomeAudience.Administrator
            if (changingPassword) {
                val passwordModel: ChangePasswordViewModel = viewModel(
                    key = "change-password-${current.user.id}-$passwordSession",
                    factory = factory { ChangePasswordViewModel(container.changePassword) }
                )
                val passwordState by passwordModel.state.collectAsStateWithLifecycle()
                ChangePasswordScreen(
                    passwordState,
                    passwordModel::onIntent,
                    onBack = { changingPassword = false },
                    onSaved = { changingPassword = false }
                )
            } else if (editingProfile) {
                val editProfileModel: EditProfileViewModel = viewModel(
                    key = "edit-profile-${current.user.id}-$editProfileSession",
                    factory = factory { EditProfileViewModel(current.user.id, container.profileRepository) }
                )
                val editProfileState by editProfileModel.state.collectAsStateWithLifecycle()
                EditProfileScreen(
                    editProfileState,
                    editProfileModel::onIntent,
                    onBack = { editingProfile = false },
                    onSaved = {
                        editingProfile = false
                        model.onIntent(HomeIntent.Refresh)
                    }
                )
            } else if (accountOpen) {
                AccountScreen(
                    onBack = { accountOpen = false },
                    onChangePassword = {
                        passwordSession++
                        changingPassword = true
                    },
                    onEditProfile = {
                        editProfileSession++
                        editingProfile = true
                    }
                )
            } else if (creating || editingAssignment != null) {
                val assignment = editingAssignment
                val creationModel: AdminCreationViewModel = viewModel(
                    key = "admin-creation-$creationSession",
                    factory = factory { AdminCreationViewModel(container.adminRepository, container.bulletinsRepository, assignment) }
                )
                val creationState by creationModel.state.collectAsStateWithLifecycle()
                AdminCreationScreen(
                    creationState,
                    creationModel::onIntent,
                    onClose = {
                        creating = false
                        editingAssignment = null
                    },
                    onCompleted = {
                        creating = false
                        editingAssignment = null
                        model.onIntent(HomeIntent.CloseDetail)
                        model.onIntent(HomeIntent.Refresh)
                    }
                )
            } else if (state.assignment != null) {
                val assignment = requireNotNull(state.assignment)
                val detailModel: AssignmentDetailViewModel = viewModel(
                    key = "assignment-detail-${assignment.id}",
                    factory = factory {
                        AssignmentDetailViewModel(
                            assignment.id,
                            current.user.id,
                            administrator,
                            container.observeAssignmentDetails,
                            container.confirmMilestone,
                            container.milestonesRepository,
                            container.adminRepository
                        )
                    }
                )
                val detailState by detailModel.state.collectAsStateWithLifecycle()
                AssignmentDetailScreen(
                    detailState,
                    current.user.id,
                    administrator,
                    detailModel::onIntent,
                    onBack = {
                        model.onIntent(HomeIntent.CloseDetail)
                        model.onIntent(HomeIntent.Refresh)
                    },
                    onEdit = {
                        creationSession++
                        editingAssignment = it
                    },
                    onDeleted = {
                        model.onIntent(HomeIntent.CloseDetail)
                        model.onIntent(HomeIntent.Refresh)
                    }
                )
            } else if (state.bulletin != null) {
                val bulletin = requireNotNull(state.bulletin)
                val detailModel: BulletinDetailViewModel = viewModel(
                    key = "bulletin-detail-${bulletin.id}",
                    factory = factory { BulletinDetailViewModel(bulletin, administrator, container.bulletinsRepository) }
                )
                val detailState by detailModel.state.collectAsStateWithLifecycle()
                BulletinDetailScreen(
                    detailState,
                    administrator,
                    detailModel::onIntent,
                    onBack = {
                        model.onIntent(HomeIntent.CloseDetail)
                        model.onIntent(HomeIntent.Refresh)
                    },
                    onDeleted = {
                        model.onIntent(HomeIntent.CloseDetail)
                        model.onIntent(HomeIntent.Refresh)
                    }
                )
            } else if (state.notificationsOpen) {
                NotificationsScreen(
                    notificationState,
                    onBack = { model.onIntent(HomeIntent.CloseNotifications) },
                    onReadAll = { notifications.markRead() },
                    onOpen = { notifications.open(it, administrator) }
                )
            } else {
                HomeScreen(
                    state.copy(unreadNotifications = notificationState.unread),
                    model::onIntent,
                    onCreateAssignment = {
                        creationSession++
                        editingAssignment = null
                        creating = true
                    },
                    onOpenAccount = { accountOpen = true }
                )
            }
        }
    }
}

@Composable
private fun SessionScope(identity: String, sessionModel: SessionViewModel, content: @Composable () -> Unit) {
    key(identity) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides sessionModel.scope(identity), content = content)
    }
}
