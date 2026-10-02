package edu.ucne.soundsicoappandroid.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.domain.model.Assignment
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HomeHeader(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Text("CENTRO DE ASIGNACIONES", fontSize = 10.sp, letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Hola, ${state.profile?.username?.takeIf { it.isNotBlank() } ?: state.user.displayName}",
                    fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
            }
            IconButton({ onIntent(HomeIntent.Refresh) }, enabled = !state.loading) {
                Icon(Icons.Outlined.Refresh, "Actualizar", Modifier.size(21.dp))
            }
            FilledIconButton({ onIntent(HomeIntent.ToggleSearch) }, modifier = Modifier.size(40.dp)) {
                Icon(if (state.searching) Icons.Outlined.Close else Icons.Outlined.Search, if (state.searching) "Cerrar búsqueda" else "Buscar")
            }
        }
        if (state.searching) OutlinedTextField(
            state.search, { onIntent(HomeIntent.Search(it)) }, Modifier.fillMaxWidth(),
            label = { Text("Buscar asignaciones") }, singleLine = true, enabled = !state.loading
        )
    }
}

@Composable
internal fun AssignmentSectionHeader(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Asignaciones actuales", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton({ onIntent(HomeIntent.ShowAll) }, enabled = !state.loading) {
                Text("Ver todas (${state.content.dashboard?.metrics?.total ?: state.content.assignments.size})", fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssignmentFilter.entries.forEach { filter ->
                FilterChip(
                    state.filter == filter, { onIntent(HomeIntent.Filter(filter)) },
                    label = { Text(filter.title, fontSize = 12.sp) }, enabled = !state.loading,
                    shape = CircleShape, border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.surface,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

internal fun LazyListScope.assignmentItems(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    if (state.listLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    state.listFailure?.let { message ->
        item {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton({ onIntent(HomeIntent.RetryAssignments) }) { Text("Reintentar asignaciones") }
        }
    }
    if (state.visibleAssignments.isEmpty() && !state.loading && !state.listLoading && state.failure == null && state.listFailure == null) item {
        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.EventAvailable, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Sin asignaciones", fontWeight = FontWeight.SemiBold)
            Text(if (state.audience == HomeAudience.Employee && state.todayOnly)
                "No tienes asignaciones para hoy." else "No hay asignaciones que coincidan con los filtros.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
    items(state.visibleAssignments, key = { it.id }) { assignment ->
        AssignmentCard(assignment) { onIntent(HomeIntent.OpenAssignment(assignment)) }
    }
}

@Composable
internal fun AssignmentCard(assignment: Assignment, onClick: () -> Unit) {
    Card(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp)) {
                    Icon(if (assignment.type == "campo") Icons.Outlined.LocalShipping else Icons.Outlined.Description,
                        null, Modifier.padding(10.dp).size(23.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (assignment.type == "campo") "Campo" else "Oficina", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (assignment.priority.isNotBlank()) Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(3.dp)) {
                            Text(assignment.priority.uppercase(Locale.forLanguageTag("es")), Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(assignment.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                val statusColor = when (assignment.status) {
                    "completada" -> Color(0xFF5EBB66)
                    "vencida" -> Color(0xFFEF6670)
                    "en_curso" -> Color(0xFF679CEE)
                    else -> Color(0xFFE8D75C)
                }
                Surface(color = statusColor.copy(alpha = 0.23f), shape = CircleShape) {
                    Text(statusLabel(assignment.status), Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        val date = assignment.nextMilestoneDate ?: assignment.deadline
                        Text(date?.let(::homeTimeLabel) ?: "Horario por confirmar", fontSize = 11.sp)
                        assignment.nextMilestoneTitle?.let {
                            Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                    if (assignment.type == "campo") Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.LocationOn, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(assignment.location?.takeIf(String::isNotBlank) ?: "Ubicación por confirmar",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Detalle", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun homeTimeLabel(value: String) = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.forLanguageTag("es")))
}.getOrDefault(value)

internal fun statusLabel(value: String) = when (value) {
    "completada" -> "Completada"
    "vencida" -> "Vencida"
    "en_curso" -> "En curso"
    else -> "Pendiente"
}
