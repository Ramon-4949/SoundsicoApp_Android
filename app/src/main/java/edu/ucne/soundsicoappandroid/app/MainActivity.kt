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
import edu.ucne.soundsicoappandroid.domain.model.SessionState
import edu.ucne.soundsicoappandroid.presentation.home.*
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
                HomeViewModel(current.user, container.getProfile, container.getHomeContent, container.signOut)
            })
            val state by model.state.collectAsStateWithLifecycle()
            HomeScreen(state, model::onIntent)
        }
    }
}

@Composable
private fun SessionScope(identity: String, sessionModel: SessionViewModel, content: @Composable () -> Unit) {
    key(identity) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides sessionModel.scope(identity), content = content)
    }
}
