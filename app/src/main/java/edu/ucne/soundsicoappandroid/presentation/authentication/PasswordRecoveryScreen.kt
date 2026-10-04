package edu.ucne.soundsicoappandroid.presentation.authentication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.core.designsystem.AuthField
import edu.ucne.soundsicoappandroid.core.designsystem.FailureDialog
import edu.ucne.soundsicoappandroid.core.designsystem.PrimaryAction

@Composable
fun PasswordRecoveryScreen(
    state: PasswordRecoveryState,
    onIntent: (PasswordRecoveryIntent) -> Unit,
    onClose: () -> Unit
) {
    val closeOrBack = {
        if (state.step == PasswordRecoveryStep.Request || state.step == PasswordRecoveryStep.Complete) onClose()
        else onIntent(PasswordRecoveryIntent.Previous)
    }
    BackHandler(enabled = true) { if (!state.busy) closeOrBack() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().imePadding()) {
            IconButton(closeOrBack, enabled = !state.busy, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).widthIn(max = 520.dp).fillMaxWidth()
                    .align(Alignment.CenterHorizontally).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state.step) {
                    PasswordRecoveryStep.Request -> RecoveryRequest(state, onIntent, onClose)
                    PasswordRecoveryStep.Verification -> RecoveryVerification(state, onIntent, onClose)
                    PasswordRecoveryStep.Password -> RecoveryPassword(state, onIntent)
                    PasswordRecoveryStep.Complete -> RecoveryComplete(onClose)
                }
            }
        }
    }
    FailureDialog(state.failure) { onIntent(PasswordRecoveryIntent.DismissError) }
}

@Composable
private fun RecoveryHeading(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Surface(Modifier.size(72.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary) }
    }
    Spacer(Modifier.height(18.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun RecoveryRequest(state: PasswordRecoveryState, onIntent: (PasswordRecoveryIntent) -> Unit, onClose: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    RecoveryHeading(Icons.Outlined.LockReset, "Recuperar Contraseña", "Introduce tu correo corporativo registrado. Te enviaremos un código seguro para restablecer tu acceso.")
    Spacer(Modifier.height(22.dp))
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AuthField("Correo electrónico registrado", state.email, { onIntent(PasswordRecoveryIntent.EmailChanged(it)) }, !state.busy, state.errors["email"], keyboardType = KeyboardType.Email)
            Text("MÉTODO DE RECUPERACIÓN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RecoveryMethodRow(Icons.Outlined.Email, "Correo Electrónico", state.email)
        }
    }
    Spacer(Modifier.height(42.dp))
    PrimaryAction(if (state.busy) "Enviando…" else "Enviar Enlace de Recuperación", state.busy) { onIntent(PasswordRecoveryIntent.SubmitRequest) }
    Spacer(Modifier.height(10.dp))
    TextButton(onClose, enabled = !state.busy) { Text("¿Recordaste tu contraseña?  Iniciar sesión") }
}

@Composable
private fun RecoveryMethodRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(38.dp), shape = RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surface) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail.ifBlank { "Tu correo registrado" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            RadioButton(true, onClick = null)
        }
    }
}

@Composable
private fun RecoveryVerification(state: PasswordRecoveryState, onIntent: (PasswordRecoveryIntent) -> Unit, onClose: () -> Unit) {
    val requesters = remember { List(6) { FocusRequester() } }
    Spacer(Modifier.height(18.dp))
    RecoveryHeading(Icons.Outlined.MarkEmailRead, "Verifica tu Identidad", "Hemos enviado un código de verificación de 6 dígitos a tu correo corporativo registrado")
    Spacer(Modifier.height(8.dp))
    AssistChip(onClick = {}, label = { Text(state.sentEmail) }, leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null, Modifier.size(16.dp)) })
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)) {
        repeat(6) { index ->
            val value = state.code.getOrNull(index)?.takeIf(Char::isDigit)?.toString().orEmpty()
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onIntent(PasswordRecoveryIntent.DigitChanged(index, it))
                    if (it.any(Char::isDigit) && index < 5) requesters[index + 1].requestFocus()
                },
                modifier = Modifier.width(48.dp).focusRequester(requesters[index]),
                enabled = !state.busy,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = if (index == 5) ImeAction.Done else ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
    state.errors["code"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
    Spacer(Modifier.height(12.dp))
    val minutes = state.expiresIn / 60
    val seconds = state.expiresIn % 60
    Text("◷ El código expira en %02d:%02d".format(minutes, seconds), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))
    Text("¿No recibiste el código?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    TextButton({ onIntent(PasswordRecoveryIntent.Resend) }, enabled = !state.busy && state.resendIn == 0) {
        Text(if (state.resendIn > 0) "Reenviar código (${state.resendIn}s)" else "Reenviar código")
    }
    Spacer(Modifier.height(38.dp))
    PrimaryAction(if (state.busy) "Verificando…" else "Verificar y Continuar", state.busy) { onIntent(PasswordRecoveryIntent.Verify) }
    TextButton({ onIntent(PasswordRecoveryIntent.Previous) }, enabled = !state.busy) { Text("Probar otro método de recuperación") }
    TextButton(onClose, enabled = !state.busy) { Text("Volver a Iniciar Sesión") }
}

@Composable
private fun RecoveryPassword(state: PasswordRecoveryState, onIntent: (PasswordRecoveryIntent) -> Unit) {
    Spacer(Modifier.height(18.dp))
    RecoveryHeading(Icons.Outlined.LockReset, "Restablecer contraseña", "Establece una nueva clave segura para tu cuenta en SounDisco. Debe ser diferente a tus contraseñas anteriores.")
    Spacer(Modifier.height(20.dp))
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AuthField("Nueva contraseña", state.password, { onIntent(PasswordRecoveryIntent.PasswordChanged(it)) }, !state.busy, state.errors["password"], password = true)
            LinearProgressIndicator(progress = { state.passwordChecks.count { it.second } / 4f }, Modifier.fillMaxWidth())
            AuthField("Confirmar nueva contraseña", state.confirmation, { onIntent(PasswordRecoveryIntent.ConfirmationChanged(it)) }, !state.busy, state.errors["confirmation"], password = true, imeAction = ImeAction.Done, onDone = { onIntent(PasswordRecoveryIntent.SubmitPassword) })
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
    PrimaryAction(if (state.busy) "Actualizando…" else "Restablecer Contraseña", state.busy) { onIntent(PasswordRecoveryIntent.SubmitPassword) }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun RecoveryComplete(onClose: () -> Unit) {
    Spacer(Modifier.height(64.dp))
    RecoveryHeading(Icons.Outlined.VerifiedUser, "Contraseña actualizada", "Tu nueva contraseña ya está activa. Puedes iniciar sesión con tus nuevas credenciales.")
    Spacer(Modifier.height(36.dp))
    Button(onClose, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Volver a Iniciar Sesión", fontWeight = FontWeight.Bold) }
}
