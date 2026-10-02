package edu.ucne.soundsicoappandroid.presentation.assignmentdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.domain.model.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(
    state: AssignmentDetailState,
    userId: String,
    administrator: Boolean,
    onIntent: (AssignmentDetailIntent) -> Unit,
    onBack: () -> Unit,
    onEdit: (Assignment) -> Unit,
    onDeleted: () -> Unit
) {
    var checklistOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }
    if (checklistOpen && state.details != null) {
        AssignmentChecklistScreen(
            requireNotNull(state.details),
            state,
            userId,
            administrator,
            onIntent,
            onBack = { checklistOpen = false },
            onFinished = { checklistOpen = false }
        )
        return
    }
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de asignación", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary) }
                },
                actions = {
                    if (administrator && state.details != null) {
                        IconButton({ onEdit(state.details.assignment) }) { Icon(Icons.Outlined.Edit, "Editar") }
                        IconButton({ onIntent(AssignmentDetailIntent.RequestDelete) }) {
                            Icon(Icons.Outlined.DeleteOutline, "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading && state.details == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.details?.let { details ->
                AssignmentDetailContent(details, state, userId, administrator, onIntent) { checklistOpen = true }
            }
        }
    }
    if (state.deleteConfirmation) AlertDialog(
        onDismissRequest = { onIntent(AssignmentDetailIntent.CancelDelete) },
        icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Eliminar asignación") },
        text = { Text("Esta acción eliminará la asignación, sus hitos y las relaciones con colaboradores.") },
        dismissButton = { TextButton({ onIntent(AssignmentDetailIntent.CancelDelete) }) { Text("Cancelar") } },
        confirmButton = {
            Button(
                { onIntent(AssignmentDetailIntent.ConfirmDelete) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Eliminar") }
        }
    )
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { onIntent(AssignmentDetailIntent.DismissError) },
            title = { Text("No se pudo completar la operación") },
            text = { Text(message) },
            confirmButton = { TextButton({ onIntent(AssignmentDetailIntent.DismissError) }) { Text("Entendido") } }
        )
    }
}

@Composable
private fun AssignmentDetailContent(
    details: AssignmentDetails,
    state: AssignmentDetailState,
    userId: String,
    administrator: Boolean,
    onIntent: (AssignmentDetailIntent) -> Unit,
    onOpenChecklist: () -> Unit
) {
    val assignment = details.assignment
    val milestones = if (administrator) assignment.milestones else assignment.milestones.filter { milestone ->
        milestone.collaborators.any { it.userId == userId }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DetailSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        DetailLabel("ASIGNACIÓN")
                        Text(assignment.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (assignment.type == "campo") "Operaciones de campo" else "Tarea administrativa", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                        Icon(
                            if (assignment.type == "campo") Icons.Outlined.LocalShipping else Icons.AutoMirrored.Outlined.Assignment,
                            null,
                            Modifier.padding(12.dp).size(25.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        assignment.location?.takeIf(String::isNotBlank)?.let { location ->
            item {
                DetailSurface {
                    DetailLabel("UBICACIÓN DEL EVENTO")
                    Text(location, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            DetailSurface {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    DetailLabel(if (administrator) "CRONOGRAMA OPERATIVO" else "MIS HITOS ASIGNADOS")
                }
                Spacer(Modifier.height(8.dp))
                if (milestones.isEmpty()) Text("No hay hitos asignados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                milestones.sortedBy { it.order }.forEach { milestone ->
                    MilestoneDetailRow(milestone, state, userId)
                }
            }
        }
        if (details.collaborators.isNotEmpty()) item {
            DetailSurface {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Outlined.Groups, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                    DetailLabel("COLABORADORES ASIGNADOS")
                }
                Spacer(Modifier.height(9.dp))
                details.collaborators.sortedByDescending { it.supervisor }.forEach { collaborator ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(38.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(collaborator.name.initials(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(collaborator.name, fontWeight = FontWeight.SemiBold)
                            Text(collaborator.position.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (collaborator.supervisor) AssistChip(onClick = {}, label = { Text("Supervisor", fontSize = 9.sp) })
                    }
                }
            }
        }
        assignment.instructions?.takeIf(String::isNotBlank)?.let { instructions ->
            item {
                DetailSurface {
                    Text("Notas e incidencias", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(9.dp)) {
                        Text(instructions, Modifier.fillMaxWidth().padding(12.dp))
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().clickable(onClick = onOpenChecklist)) {
                DetailSurface {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Checklist, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Checklist", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "${milestones.count { it.isConfirmed(details, userId, administrator) }} / ${milestones.size}",
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        if (details.notes.isNotEmpty()) item {
            DetailSurface {
                DetailLabel("ACTIVIDAD Y NOTAS")
                details.notes.forEach { note ->
                    Column(Modifier.padding(vertical = 7.dp)) {
                        Text(note.authorName ?: "Colaborador", fontWeight = FontWeight.SemiBold)
                        Text(note.content)
                        Text(detailDate(note.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            DetailSurface {
                DetailLabel("AÑADIR NOTA")
                OutlinedTextField(
                    state.note,
                    { onIntent(AssignmentDetailIntent.ChangeNote(it)) },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe una nota o incidencia") },
                    minLines = 2,
                    trailingIcon = {
                        IconButton({ onIntent(AssignmentDetailIntent.AddNote) }, enabled = state.note.trim().length >= 3 && !state.savingNote) {
                            Icon(Icons.AutoMirrored.Outlined.Send, "Guardar nota")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MilestoneDetailRow(
    milestone: Milestone,
    state: AssignmentDetailState,
    userId: String
) {
    val collaborator = milestone.collaborators.firstOrNull { it.userId == userId }
    val confirmed = collaborator?.confirmed == true || state.details?.checkIns?.any { it.milestoneId == milestone.id } == true
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(milestone.scheduledAt?.let(::detailDate) ?: milestone.estimatedTime.orEmpty(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(5.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.fillMaxSize(), shape = CircleShape, color = if (confirmed || milestone.completed) Color(0xFF54B96B) else MaterialTheme.colorScheme.primary) {}
            }
            Spacer(Modifier.width(8.dp))
            Text(milestone.title, Modifier.weight(1f), fontSize = 12.sp)
            if (confirmed) Icon(Icons.Outlined.CheckCircle, "Confirmado", tint = Color(0xFF42A85A), modifier = Modifier.size(19.dp))
        }
        HorizontalDivider(Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun DetailSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun DetailLabel(value: String) {
    Text(value, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
}

private fun String.initials() = split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")

private fun detailDate(value: String) = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy · h:mm a", Locale.forLanguageTag("es")))
}.getOrDefault(value)
