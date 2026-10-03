package edu.ucne.soundsicoappandroid.presentation.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.domain.model.EmployeeNotification
import java.time.*
import java.time.format.DateTimeFormatter

enum class NotificationFilter(val title: String) {
    All("Todas"), Unread("Sin leer"), Assignments("Asignaciones"), Bulletins("Comunicados"),
    Overdue("Vencidas"), Pending("Pendientes"), Completed("Completadas");

    fun matches(item: EmployeeNotification): Boolean = when (this) {
        All -> true
        Unread -> !item.read
        Assignments -> item.destinationType == "asignacion"
        Bulletins -> item.destinationType == "comunicado"
        Overdue -> item.status == "vencida"
        Pending -> item.status in listOf("pendiente", "en_curso")
        Completed -> item.status == "completada"
    }
}

@Composable
fun NotificationsScreen(
    state: NotificationsState, onBack: () -> Unit, onRefresh: () -> Unit,
    onReadAll: () -> Unit, onOpen: (String) -> Unit,
    pushStatus: String?, onEnablePush: () -> Unit
) {
    BackHandler(onBack = onBack)
    var filter by rememberSaveable { mutableStateOf(NotificationFilter.All) }
    val today = LocalDate.now()
    val groups = state.items.filter(filter::matches).groupBy {
        runCatching { OffsetDateTime.parse(it.createdAt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary) } }
        item {
            Text("Notificaciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Centro de Operaciones y Control de notificaciones", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row {
                TextButton(onRefresh) { Text("Actualizar") }
                TextButton(onReadAll, enabled = state.unread > 0) { Text("Marcar todas como leídas") }
            }
            pushStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            TextButton(onEnablePush) { Text("Configurar avisos del dispositivo") }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NotificationFilter.entries) { value ->
                    FilterChip(filter == value, { filter = value },
                        label = { Text("${value.title} (${state.items.count(value::matches)})") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                }
            }
        }
        if (state.loading || state.opening) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.failure?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        state.information?.let { item { Text(it) } }
        if (groups.isEmpty() && !state.loading && state.failure == null) item { Text("No hay notificaciones para este filtro.") }
        groups.forEach { (date, notifications) ->
            item {
                Text(when (date) {
                    today -> "HOY"
                    today.minusDays(1) -> "AYER"
                    null -> "ANTERIORES"
                    else -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }, style = MaterialTheme.typography.labelSmall)
            }
            items(notifications, key = { it.id }) { notification ->
                Surface(shape = RoundedCornerShape(16.dp),
                    border = if (!notification.read) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                            Text(notification.title ?: "SounDisco", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            if (!notification.read) Badge { Text("Nueva") }
                        }
                        Text(notification.message.orEmpty())
                        Text(runCatching { OffsetDateTime.parse(notification.createdAt).atZoneSameInstant(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault(""), style = MaterialTheme.typography.labelSmall)
                        Button({ onOpen(notification.id) }, Modifier.fillMaxWidth(), enabled = !state.opening) {
                            Text(if (notification.destinationType == "asignacion") "Ver asignación y checklist" else "Ver detalles")
                        }
                    }
                }
            }
        }
    }
}
