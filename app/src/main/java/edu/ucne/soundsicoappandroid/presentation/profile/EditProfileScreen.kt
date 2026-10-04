package edu.ucne.soundsicoappandroid.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.core.designsystem.FailureDialog
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    state: EditProfileState,
    onIntent: (EditProfileIntent) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var discardConfirmation by remember { mutableStateOf(false) }
    var positionsOpen by remember { mutableStateOf(false) }
    val requestBack = { if (state.changed) discardConfirmation = true else onBack() }
    BackHandler(enabled = !state.saving) { requestBack() }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().imePadding()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(requestBack, enabled = !state.saving) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary) }
                Text("Editar Perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).widthIn(max = 520.dp).fillMaxWidth()
                    .align(Alignment.CenterHorizontally).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(Modifier.size(94.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AccountCircle, null, Modifier.size(82.dp), tint = MaterialTheme.colorScheme.outline) }
                }
                OutlinedTextField(
                    value = state.profile?.name.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    label = { Text("Nombre Completo") },
                    leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                    trailingIcon = { Icon(Icons.Outlined.Lock, "Bloqueado") },
                    shape = RoundedCornerShape(14.dp)
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("El nombre completo está bloqueado por políticas de la empresa. Contacta a Recursos Humanos o IT para solicitar cambios.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = state.username,
                    onValueChange = { onIntent(EditProfileIntent.UsernameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.saving,
                    label = { Text("Nombre de Usuario") },
                    leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = if (state.username.isNotEmpty()) {
                        { IconButton({ onIntent(EditProfileIntent.UsernameChanged("")) }) { Icon(Icons.Outlined.Cancel, "Borrar") } }
                    } else null,
                    isError = state.errors["username"] != null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                state.errors["username"]?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onIntent(EditProfileIntent.PhoneChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.saving,
                    label = { Text("Teléfono") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = state.errors["phone"] != null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                state.errors["phone"]?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                ExposedDropdownMenuBox(positionsOpen, { positionsOpen = !positionsOpen && !state.saving }) {
                    OutlinedTextField(
                        value = state.position,
                        onValueChange = {},
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        readOnly = true,
                        enabled = !state.saving,
                        label = { Text("Cargo") },
                        leadingIcon = { Icon(Icons.Outlined.Tune, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(positionsOpen) },
                        isError = state.errors["position"] != null,
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(positionsOpen, { positionsOpen = false }) {
                        AuthValidation.positions.forEach { position ->
                            DropdownMenuItem({ Text(position) }, onClick = {
                                onIntent(EditProfileIntent.PositionChanged(position))
                                positionsOpen = false
                            })
                        }
                    }
                }
                state.errors["position"]?.let { Text(it, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(24.dp))
                Button(
                    { onIntent(EditProfileIntent.Save) },
                    Modifier.fillMaxWidth().height(54.dp),
                    enabled = state.changed && !state.saving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Outlined.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.saving) "Guardando…" else "Guardar Cambios", fontWeight = FontWeight.Bold)
                }
                TextButton(requestBack, enabled = !state.saving) { Text("Descartar Cambios") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (discardConfirmation) AlertDialog(
        onDismissRequest = { discardConfirmation = false },
        title = { Text("¿Descartar los cambios?") },
        text = { Text("Los cambios sin guardar se perderán.") },
        confirmButton = { TextButton({ discardConfirmation = false; onBack() }) { Text("Descartar") } },
        dismissButton = { TextButton({ discardConfirmation = false }) { Text("Seguir editando") } }
    )
    FailureDialog(state.failure) { onIntent(EditProfileIntent.DismissError) }
}
