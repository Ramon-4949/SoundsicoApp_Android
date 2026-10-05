package edu.ucne.soundsicoappandroid.presentation.assignmentdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.domain.model.AssignmentDetails
import edu.ucne.soundsicoappandroid.domain.model.Milestone
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentChecklistScreen(
    details: AssignmentDetails,
    state: AssignmentDetailState,
    userId: String,
    administrator: Boolean,
    onIntent: (AssignmentDetailIntent) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var undoTarget by remember { mutableStateOf<Milestone?>(null) }
    val oversight = details.canOversee(userId, administrator)
    val milestones = details.assignment.milestones
        .filter { oversight || it.collaborators.any { collaborator -> collaborator.userId == userId } }
        .sortedBy { it.order }
    val completion = milestones.associateWith { milestone -> milestone.isConfirmed(details, userId, oversight) }
    val activeIndex = milestones.indexOfFirst { completion[it] != true }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (milestones.isEmpty()) {
                item {
                    Text(
                        "No tienes hitos asignados en esta asignación.",
                        Modifier.fillMaxWidth().padding(top = 40.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(milestones, key = { _, milestone -> milestone.id }) { index, milestone ->
                val confirmed = completion[milestone] == true
                val active = index == activeIndex
                val locked = !confirmed && !active
                if (oversight) TeamMilestone(milestone, details, index + 1, active, index < milestones.lastIndex)
                else ChecklistMilestone(
                    milestone,
                    index + 1,
                    confirmed,
                    active,
                    locked,
                    index < milestones.lastIndex,
                    state.checkingMilestoneId == milestone.id,
                    administrator,
                    milestone.collaborators.firstOrNull { it.userId == userId }?.deliveryLabel() ?: "Sin confirmar",
                    onConfirm = { onIntent(AssignmentDetailIntent.CheckIn(milestone)) },
                    onUndo = { undoTarget = milestone }
                )
            }
        }
    }
    undoTarget?.let { milestone ->
        AlertDialog(
            onDismissRequest = { undoTarget = null },
            title = { Text("¿Deshacer tu confirmación?") },
            text = { Text("Este hito volverá a aparecer como pendiente para ti.") },
            confirmButton = {
                TextButton({
                    undoTarget = null
                    onIntent(AssignmentDetailIntent.UndoCheckIn(milestone))
                }) { Text("Deshacer") }
            },
            dismissButton = { TextButton({ undoTarget = null }) { Text("Cancelar") } }
        )
    }
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { onIntent(AssignmentDetailIntent.DismissError) },
            title = { Text("No se pudo completar la acción") },
            text = { Text(message) },
            confirmButton = { TextButton({ onIntent(AssignmentDetailIntent.DismissError) }) { Text("Entendido") } }
        )
    }
}

@Composable
private fun ChecklistMilestone(
    milestone: Milestone,
    number: Int,
    confirmed: Boolean,
    active: Boolean,
    locked: Boolean,
    hasNext: Boolean,
    checking: Boolean,
    administrator: Boolean,
    delivery: String,
    onConfirm: () -> Unit,
    onUndo: () -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                Modifier.size(26.dp),
                shape = CircleShape,
                color = when {
                    active -> MaterialTheme.colorScheme.primary
                    confirmed -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        confirmed -> Icon(Icons.Outlined.Check, "Completado", Modifier.size(16.dp))
                        locked -> Icon(Icons.Outlined.Lock, "Bloqueado", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            if (hasNext) Box(
                Modifier.width(2.dp).height(if (active) 112.dp else 54.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        if (active) {
            Surface(
                Modifier.weight(1f).padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MilestoneHeader(milestone, Color.Unspecified)
                    Text(delivery, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!administrator) Button(
                        onConfirm,
                        Modifier.fillMaxWidth().height(46.dp),
                        enabled = !checking
                    ) {
                        if (checking) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                        else {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(19.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Confirmar")
                        }
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f).padding(top = 3.dp, bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MilestoneHeader(
                    milestone,
                    if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f) else Color.Unspecified
                )
                Text(delivery, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (confirmed && !administrator) TextButton(
                    onUndo,
                    enabled = !checking,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    if (checking) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else {
                        Icon(Icons.AutoMirrored.Outlined.Undo, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Deshacer confirmación")
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneHeader(milestone: Milestone, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(milestone.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = color)
        milestone.scheduledAt?.let {
            Text(checklistTime(it), fontSize = 11.sp, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color)
        }
    }
}

internal fun Milestone.isConfirmed(details: AssignmentDetails, userId: String, administrator: Boolean): Boolean {
    if (administrator) return globallyCompleted
    return collaborators.any { it.userId == userId && it.confirmed } ||
        details.checkIns.any { it.milestoneId == id && it.userId == userId }
}

private fun checklistTime(value: String) = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
}.getOrDefault(value)

internal fun AssignmentDetails.canOversee(userId: String, administrator: Boolean): Boolean =
    administrator || assignment.supervisors.any { it.userId == userId } ||
        collaborators.any { it.id == userId && it.supervisor }

@Composable
private fun TeamMilestone(
    milestone: Milestone,
    details: AssignmentDetails,
    number: Int,
    active: Boolean,
    hasNext: Boolean
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(36.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                Modifier.size(26.dp),
                shape = CircleShape,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (milestone.globallyCompleted) Icon(Icons.Outlined.Check, "Completado", Modifier.size(16.dp))
                    else Text(number.toString(), color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (hasNext) Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
        }
        Column(Modifier.weight(1f).padding(start = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MilestoneHeader(milestone, Color.Unspecified)
            if (active) Text("En curso", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (milestone.collaborators.isEmpty()) Text("Sin colaboradores asignados", fontSize = 12.sp)
                    milestone.collaborators.forEach { member ->
                        val name = member.employee?.name?.takeIf { it.isNotBlank() }
                            ?: details.collaborators.firstOrNull { it.id == member.userId }?.name ?: "Colaborador"
                        val tint = when {
                            !member.confirmed -> MaterialTheme.colorScheme.error
                            member.status == "temprano" -> Color(0xFF168568)
                            member.status == "tardio" -> Color(0xFFAD6500)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (member.confirmed) "✓" else "!", color = tint, modifier = Modifier.padding(end = 6.dp))
                            Text(name, Modifier.weight(1f), fontSize = 12.sp)
                            Surface(shape = RoundedCornerShape(20.dp), color = tint.copy(alpha = 0.1f)) {
                                Text(member.deliveryLabel(), Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = tint, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun edu.ucne.soundsicoappandroid.domain.model.MilestoneCollaborator.deliveryLabel(): String {
    if (!confirmed) return "Sin confirmar"
    val seconds = runCatching {
        kotlin.math.abs(java.time.Duration.between(OffsetDateTime.parse(scheduledAt), OffsetDateTime.parse(confirmedAt)).seconds)
    }.getOrNull()
    val duration = seconds?.let(::formatChecklistDuration)
    return when (status) {
        "temprano" -> "Temprano" + (seconds?.let { if (it >= 60) " (-$duration)" else " (<1 min)" } ?: "")
        "tardio" -> "Tardío" + (seconds?.let { if (it >= 60) " (+$duration)" else " (<1 min)" } ?: "")
        else -> "A tiempo"
    }
}

private fun formatChecklistDuration(totalSeconds: Long): String {
    var minutes = totalSeconds / 60
    val units = listOf(
        525_600L to "a",
        43_200L to "mes",
        10_080L to "sem",
        1_440L to "d",
        60L to "h",
        1L to "min"
    )
    val parts = mutableListOf<String>()
    for ((size, label) in units) {
        if (minutes >= size) {
            val amount = minutes / size
            parts += "$amount $label"
            minutes %= size
            if (parts.size == 2) break
        }
    }
    return parts.joinToString(" ").ifBlank { "<1 min" }
}
