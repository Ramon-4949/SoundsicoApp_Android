package edu.ucne.soundsicoappandroid.app

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SoundiscoApplication).container
        setContent {
            SoundiscoTheme { SoundiscoApp(container) }
        }
    }
}

private inline fun <reified T : ViewModel> factory(crossinline create: () -> T) = object : ViewModelProvider.Factory {
    override fun <V : ViewModel> create(modelClass: Class<V>): V = modelClass.cast(create()) as V
}

@Composable
private fun SoundiscoApp(container: AppContainer) {
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
            var registration by rememberSaveable { mutableStateOf(false) }
            if (registration) {
                val model: SignUpViewModel = viewModel(factory = factory { SignUpViewModel(container.signUp) })
                val state by model.state.collectAsStateWithLifecycle()
                SignUpScreen(state, model::onIntent) { registration = false }
            } else {
                val model: LoginViewModel = viewModel(factory = factory { LoginViewModel(container.login, container.loginPreferences) })
                val state by model.state.collectAsStateWithLifecycle()
                LoginScreen(state, model::onIntent) { registration = true }
            }
        }
        is SessionState.SignedIn -> SessionScope(current.user.id, sessionModel) {
            val model: HomeViewModel = viewModel(factory = factory {
                HomeViewModel(
                    current.user,
                    container.getProfile,
                    container.getHomeContent,
                    container.signOut,
                    container.adminRepository,
                    container.deleteAccount,
                    container.loginPreferences
                )
            })
            val state by model.state.collectAsStateWithLifecycle()
            var creating by rememberSaveable { mutableStateOf(false) }
            var creationSession by rememberSaveable { mutableIntStateOf(0) }
            var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
            val administrator = state.audience == HomeAudience.Administrator
            if (creating || editingAssignment != null) {
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
            } else {
                HomeScreen(state, model::onIntent) {
                    creationSession++
                    editingAssignment = null
                    creating = true
                }
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
