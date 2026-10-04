package edu.ucne.soundsicoappandroid.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.core.designsystem.AuthField
import edu.ucne.soundsicoappandroid.core.designsystem.FailureDialog
import edu.ucne.soundsicoappandroid.core.designsystem.PrimaryAction

@Composable
fun ChangePasswordScreen(
    state: ChangePasswordState,
    onIntent: (ChangePasswordIntent) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().imePadding()) {
            IconButton(onBack, enabled = !state.saving, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).widthIn(max = 520.dp).fillMaxWidth()
                    .align(Alignment.CenterHorizontally).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(Modifier.size(72.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LockReset, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(18.dp))
                Text("Nueva Contraseña", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Establece una nueva clave segura para tu cuenta en SounDisco. Debe ser diferente a tus contraseñas anteriores.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AuthField("Contraseña actual", state.currentPassword, { onIntent(ChangePasswordIntent.CurrentChanged(it)) }, !state.saving, state.errors["current"], password = true)
                        HorizontalDivider()
                        AuthField("Nueva contraseña", state.newPassword, { onIntent(ChangePasswordIntent.PasswordChanged(it)) }, !state.saving, state.errors["password"], password = true)
                        LinearProgressIndicator(progress = { state.passwordChecks.count { it.second } / 4f }, Modifier.fillMaxWidth())
                        AuthField("Confirmar nueva contraseña", state.confirmation, { onIntent(ChangePasswordIntent.ConfirmationChanged(it)) }, !state.saving, state.errors["confirmation"], password = true, imeAction = ImeAction.Done, onDone = { onIntent(ChangePasswordIntent.Save) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("REQUISITOS DE LA APLICACIÓN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.passwordChecks.forEach { (label, valid) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (valid) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(18.dp), tint = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryAction(if (state.saving) "Actualizando…" else "Actualizar Contraseña", state.saving) { onIntent(ChangePasswordIntent.Save) }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    FailureDialog(state.failure) { onIntent(ChangePasswordIntent.DismissError) }
}
