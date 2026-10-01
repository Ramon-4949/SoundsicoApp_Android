package edu.ucne.soundsicoappandroid.presentation.signup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.core.designsystem.*
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation

@Composable
fun SignUpScreen(state: SignUpState, onIntent: (SignUpIntent) -> Unit, onBack: () -> Unit) {
    BackHandler(enabled = true) { if (!state.busy) onBack() }
    var positionsOpen by remember { mutableStateOf(false) }
    var legalTitle by remember { mutableStateOf<String?>(null) }
    val form = state.registration
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                IconButton(onBack, enabled = !state.busy) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Crear cuenta", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Únete al equipo de SounDisco.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AuthField("Nombre de usuario", form.username, { onIntent(SignUpIntent.FieldChanged(SignUpField.Username, it)) }, !state.busy, state.errors["username"])
                AuthField("Nombre completo", form.fullName, { onIntent(SignUpIntent.FieldChanged(SignUpField.FullName, it)) }, !state.busy, state.errors["fullName"])
                AuthField("Correo electrónico", form.email, { onIntent(SignUpIntent.FieldChanged(SignUpField.Email, it)) }, !state.busy, state.errors["email"], keyboardType = KeyboardType.Email)
                AuthField("Número de teléfono", form.phone, { onIntent(SignUpIntent.FieldChanged(SignUpField.Phone, it)) }, !state.busy, state.errors["phone"], keyboardType = KeyboardType.Phone)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cargo en la empresa", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton({ positionsOpen = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(form.position, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ArrowDropDown, "Seleccionar cargo")
                        }
                        DropdownMenu(positionsOpen, { positionsOpen = false }) {
                            AuthValidation.positions.forEach { position ->
                                DropdownMenuItem(text = { Text(position) }, onClick = {
                                    onIntent(SignUpIntent.FieldChanged(SignUpField.Position, position))
                                    positionsOpen = false
                                })
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuthField("Contraseña", form.password, { onIntent(SignUpIntent.FieldChanged(SignUpField.Password, it)) }, !state.busy, state.errors["password"], password = true)
                    LinearProgressIndicator(
                        progress = { listOf(form.password.length >= 8, form.password.any(Char::isUpperCase) && form.password.any(Char::isLowerCase),
                            form.password.any(Char::isDigit), form.password.any { !it.isLetterOrDigit() && !it.isWhitespace() }).count { it } / 4f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Mínimo 8 caracteres, mayúscula, minúscula, número y símbolo; sin espacios.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AuthField("Confirmar contraseña", form.confirmation, { onIntent(SignUpIntent.FieldChanged(SignUpField.Confirmation, it)) }, !state.busy, state.errors["confirmation"], password = true, imeAction = ImeAction.Done, onDone = { onIntent(SignUpIntent.Submit) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Acepto los términos de servicio y la política de privacidad.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(form.acceptedTerms, { onIntent(SignUpIntent.TermsChanged(it)) }, enabled = !state.busy)
                }
                state.errors["terms"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ legalTitle = "Términos de servicio" }) { Text("Términos") }
                    TextButton({ legalTitle = "Política de privacidad" }) { Text("Privacidad") }
                }
                PrimaryAction(if (state.busy) "Creando cuenta…" else "Crear cuenta", state.busy) { onIntent(SignUpIntent.Submit) }
                TextButton(onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Ya tengo una cuenta. Iniciar sesión") }
            }
        }
    }
    FailureDialog(state.failure) { onIntent(SignUpIntent.DismissError) }
    legalTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { legalTitle = null }, title = { Text(title) },
            text = { Text("El documento corporativo aún no está disponible. Solicítalo a administración antes de crear tu cuenta.") },
            confirmButton = { TextButton({ legalTitle = null }) { Text("Entendido") } }
        )
    }
}
