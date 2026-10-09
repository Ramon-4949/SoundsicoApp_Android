package edu.ucne.soundsicoappandroid.presentation.bulletindetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.presentation.admincreation.AdminMessageComposer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulletinDetailScreen(
    state: BulletinDetailState,
    administrator: Boolean,
    onIntent: (BulletinDetailIntent) -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    BackHandler {
        if (!state.saving) {
            if (state.editing) onIntent(BulletinDetailIntent.CancelEdit) else onBack()
        }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbar.showSnackbar("Cambios guardados")
            onIntent(BulletinDetailIntent.DismissSuccess)
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing) "Editar comunicado" else "Detalle Mensaje", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton({ if (state.editing) onIntent(BulletinDetailIntent.CancelEdit) else onBack() }, enabled = !state.saving) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    if (administrator && !state.editing) {
                        IconButton({ onIntent(BulletinDetailIntent.Edit) }) { Icon(Icons.Outlined.Edit, "Editar") }
                        IconButton({ onIntent(BulletinDetailIntent.RequestDelete) }) {
                            Icon(Icons.Outlined.DeleteOutline, "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.editing) Box(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize()) {
            AdminMessageComposer(
                subject = state.subject,
                message = state.message,
                saving = state.saving,
                onSubjectChange = { onIntent(BulletinDetailIntent.ChangeSubject(it)) },
                onMessageChange = { onIntent(BulletinDetailIntent.ChangeMessage(it)) },
                onSubmit = { onIntent(BulletinDetailIntent.Save) },
                editing = true
            )
        } else Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DetailLabel("ASUNTO DEL COMUNICADO")
            DetailCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    Text(state.bulletin.subject, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                }
            }
            DetailLabel("MENSAJE / INSTRUCCIONES")
            DetailCard {
                Text(state.bulletin.message, lineHeight = 22.sp)
            }
        }
    }
    if (state.deleteConfirmation) AlertDialog(
        onDismissRequest = { onIntent(BulletinDetailIntent.CancelDelete) },
        icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Eliminar mensaje") },
        text = { Text("El mensaje dejará de estar disponible para todos los colaboradores.") },
        dismissButton = { TextButton({ onIntent(BulletinDetailIntent.CancelDelete) }) { Text("Cancelar") } },
        confirmButton = {
            Button(
                { onIntent(BulletinDetailIntent.ConfirmDelete) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Eliminar") }
        }
    )
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { onIntent(BulletinDetailIntent.DismissError) },
            title = { Text("No se pudo completar la operación") },
            text = { Text(message) },
            confirmButton = { TextButton({ onIntent(BulletinDetailIntent.DismissError) }) { Text("Entendido") } }
        )
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun DetailLabel(value: String) {
    Text(value, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
}
