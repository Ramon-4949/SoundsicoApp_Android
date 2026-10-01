package edu.ucne.soundsicoappandroid.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.core.designsystem.*

@Composable
fun LoginScreen(state: LoginState, onIntent: (LoginIntent) -> Unit, onSignUp: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BrandLogo()
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PORTAL EJECUTIVO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Bienvenido de nuevo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Ingresa tus credenciales corporativas para continuar", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        AuthField("Correo electrónico", state.email, { onIntent(LoginIntent.EmailChanged(it)) }, !state.busy,
                            state.errors["email"], keyboardType = KeyboardType.Email)
                        AuthField("Contraseña", state.password, { onIntent(LoginIntent.PasswordChanged(it)) }, !state.busy,
                            state.errors["password"], password = true, imeAction = ImeAction.Done,
                            onDone = { onIntent(LoginIntent.Submit) })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(state.rememberEmail, { onIntent(LoginIntent.RememberChanged(it)) }, enabled = !state.busy)
                            Spacer(Modifier.width(10.dp))
                            Text("Recordarme", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                PrimaryAction(if (state.busy) "Iniciando sesión…" else "Iniciar sesión", state.busy) { onIntent(LoginIntent.Submit) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                    Text("¿No tienes una cuenta?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onSignUp, enabled = !state.busy) { Text("Regístrate", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
    FailureDialog(state.failure) { onIntent(LoginIntent.DismissError) }
}
