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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    BackHandler(onBack = onBack)
    val milestones = details.assignment.milestones
        .filter { administrator || it.collaborators.any { collaborator -> collaborator.userId == userId } }
        .sortedBy { it.order }
    val completion = milestones.associateWith { milestone -> milestone.isConfirmed(details, userId, administrator) }
    val activeIndex = milestones.indexOfFirst { completion[it] != true }
    val finished = milestones.isNotEmpty() && activeIndex == -1
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
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onFinished,
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp).height(50.dp),
                    enabled = finished
                ) {
                    Text("Finalizar")
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                }
            }
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
                ChecklistMilestone(
                    milestone,
                    index + 1,
                    confirmed,
                    active,
                    locked,
                    index < milestones.lastIndex,
                    state.checkingMilestoneId == milestone.id,
                    administrator,
                    onConfirm = { onIntent(AssignmentDetailIntent.CheckIn(milestone)) }
                )
            }
        }
    }
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { onIntent(AssignmentDetailIntent.DismissError) },
            title = { Text("No se pudo confirmar el hito") },
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
    onConfirm: () -> Unit
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
            Box(Modifier.weight(1f).padding(top = 3.dp, bottom = 25.dp)) {
                MilestoneHeader(
                    milestone,
                    if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f) else Color.Unspecified
                )
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
    if (completed) return true
    if (administrator) return collaborators.isNotEmpty() && collaborators.all { it.confirmed }
    return collaborators.any { it.userId == userId && it.confirmed } ||
        details.checkIns.any { it.milestoneId == id && it.userId == userId }
}

private fun checklistTime(value: String) = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
}.getOrDefault(value)
