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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: NotificationsState,
    onBack: () -> Unit,
    onReadAll: () -> Unit,
    onOpen: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    var filter by rememberSaveable { mutableStateOf(NotificationFilter.All) }
    val today = LocalDate.now()
    val groups = state.items.filter(filter::matches).groupBy {
        runCatching { OffsetDateTime.parse(it.createdAt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary) }
                },
                actions = {
                    TextButton(onReadAll, enabled = state.unread > 0) { Text("Marcar leídas") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Centro de Operaciones y Control de notificaciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                    }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(notifications, key = { it.id }) { notification ->
                    Surface(
                        onClick = { onOpen(notification.id) },
                        enabled = !state.opening,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = if (!notification.read) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                                Text(notification.title ?: "SounDisco", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                if (!notification.read) Badge { Text("Nueva") }
                            }
                            Text(notification.message.orEmpty())
                            Text(runCatching { OffsetDateTime.parse(notification.createdAt).atZoneSameInstant(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault(""), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
