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
    var teamOpen by rememberSaveable { mutableStateOf(false) }
    var checklistOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }
    if (teamOpen && state.details != null) {
        AssignmentTeamScreen(state.details.collaborators) { teamOpen = false }
        return
    }
    if (checklistOpen && state.details != null) {
        AssignmentChecklistScreen(
            requireNotNull(state.details),
            state,
            userId,
            administrator,
            onIntent,
            onBack = { checklistOpen = false }
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
        },
        bottomBar = {
            if (state.details != null) Surface(shadowElevation = 8.dp) {
                Button(
                    { onIntent(AssignmentDetailIntent.OpenNoteEditor) },
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Outlined.EditNote, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir Nota / Reportar Incidencia")
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading && state.details == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.details?.let { details ->
                AssignmentDetailContent(details, state, userId, administrator, onOpenTeam = { teamOpen = true }) { checklistOpen = true }
            }
        }
    }
    if (state.noteEditorOpen) AssignmentNoteEditor(state, onIntent)
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
    onOpenTeam: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val assignment = details.assignment
    val oversight = details.canOversee(userId, administrator)
    val milestones = if (oversight) assignment.milestones else assignment.milestones.filter { milestone ->
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
                    DetailLabel(if (oversight) "CRONOGRAMA OPERATIVO" else "MIS HITOS ASIGNADOS")
                }
                Spacer(Modifier.height(8.dp))
                if (milestones.isEmpty()) Text("No hay hitos asignados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                milestones.sortedBy { it.order }.forEach { milestone ->
                    MilestoneDetailRow(milestone, state, userId, oversight)
                }
            }
        }
        if (details.collaborators.isNotEmpty()) item {
            AssignmentTeamSummary(details.collaborators, onOpenTeam)
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
                                "${milestones.count { it.isConfirmed(details, userId, oversight) }} / ${milestones.size}",
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

    }
}

@Composable
private fun MilestoneDetailRow(
    milestone: Milestone,
    state: AssignmentDetailState,
    userId: String,
    oversight: Boolean
) {
    val collaborator = milestone.collaborators.firstOrNull { it.userId == userId }
    val confirmed = if (oversight) milestone.globallyCompleted else
        collaborator?.confirmed == true || state.details?.checkIns?.any { it.milestoneId == milestone.id && it.userId == userId } == true
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(milestone.scheduledAt?.let(::detailDate) ?: milestone.estimatedTime.orEmpty(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(5.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.fillMaxSize(), shape = CircleShape, color = if (confirmed) Color(0xFF54B96B) else MaterialTheme.colorScheme.primary) {}
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentNoteEditor(state: AssignmentDetailState, onIntent: (AssignmentDetailIntent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(AssignmentDetailIntent.CloseNoteEditor) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    { onIntent(AssignmentDetailIntent.CloseNoteEditor) },
                    enabled = !state.savingNote
                ) { Text("Cancelar") }
                Text("Añadir nota", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.SemiBold)
                TextButton(
                    { onIntent(AssignmentDetailIntent.AddNote) },
                    enabled = state.note.trim().length >= 3 && !state.savingNote
                ) {
                    if (state.savingNote) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Publicar")
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Nota / Incidencia", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = { onIntent(AssignmentDetailIntent.ChangeNote(it)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                enabled = !state.savingNote,
                minLines = 8,
                shape = RoundedCornerShape(24.dp),
                supportingText = { Text("${state.note.length} / 4000") }
            )
        }
    }
}
