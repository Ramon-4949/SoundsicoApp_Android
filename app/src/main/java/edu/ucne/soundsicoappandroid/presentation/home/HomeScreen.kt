package edu.ucne.soundsicoappandroid.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.domain.model.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val approved = state.profile?.access == AccountAccess.Approved
    Scaffold(
        bottomBar = {
            if (approved) NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { onIntent(HomeIntent.SelectTab(tab)) },
                        icon = { Icon(when (tab) {
                            HomeTab.Start -> Icons.Outlined.Home
                            HomeTab.Messages -> Icons.AutoMirrored.Outlined.Message
                            HomeTab.Calendar -> Icons.Outlined.CalendarMonth
                            HomeTab.Profile -> Icons.Outlined.Person
                        }, tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (approved) {
                Row(Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (state.tab == HomeTab.Start) "Hola, ${state.user.displayName}" else state.tab.title,
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (state.tab == HomeTab.Start) Text(
                            if (state.profile.isAdministrator) "Panel administrativo" else "Tu agenda de trabajo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton({ onIntent(HomeIntent.Refresh) }, enabled = !state.loading) { Icon(Icons.Outlined.Refresh, "Actualizar") }
                }
            }
            state.failure?.let {
                Column(Modifier.padding(20.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    TextButton({ onIntent(HomeIntent.Refresh) }, enabled = !state.loading) { Text("Reintentar") }
                }
            }
            if (!approved) {
                AccessContent(state, onIntent)
            } else when (state.tab) {
                HomeTab.Start -> AssignmentContent(state, onIntent)
                HomeTab.Messages -> BulletinsContent(state, onIntent)
                HomeTab.Calendar -> CalendarContent(state, onIntent)
                HomeTab.Profile -> ProfileContent(state, onIntent)
            }
        }
    }
    state.assignment?.let { assignment ->
        AlertDialog(
            onDismissRequest = { onIntent(HomeIntent.CloseDetail) },
            title = { Text(assignment.title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (assignment.type == "campo") "Operaciones de campo" else "Tarea administrativa")
                    assignment.location?.takeIf(String::isNotBlank)?.let { Text(it) }
                    assignment.deadline?.let { Text("Fecha límite: ${dateLabel(it)}") }
                    assignment.instructions?.takeIf(String::isNotBlank)?.let { Text(it) }
                    Text("Estado: ${statusLabel(assignment.status)}")
                }
            },
            confirmButton = { TextButton({ onIntent(HomeIntent.CloseDetail) }) { Text("Cerrar") } }
        )
    }
    state.bulletin?.let { bulletin ->
        AlertDialog(
            onDismissRequest = { onIntent(HomeIntent.CloseDetail) },
            title = { Text(bulletin.subject) },
            text = { Text(bulletin.message, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton({ onIntent(HomeIntent.CloseDetail) }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun AccessContent(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.ManageAccounts, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(22.dp))
        Text(when {
            state.loading -> "Verificando acceso"
            state.profile?.access == AccountAccess.Rejected -> "Acceso no aprobado"
            state.profile?.access == AccountAccess.Pending -> "Cuenta en revisión"
            else -> "No se pudo verificar el acceso"
        }, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        if (state.profile?.access == AccountAccess.Pending)
            Text("Tu cuenta está creada y espera la aprobación de un administrador. La revisión tarda aproximadamente 24 horas.")
        if (state.profile?.access == AccountAccess.Rejected)
            Text("Un administrador rechazó tu solicitud. Contacta con SounDisco para consultar tu caso.")
        Spacer(Modifier.height(22.dp))
        Button({ onIntent(HomeIntent.Refresh) }, enabled = !state.loading) { Text("Consultar estado") }
        TextButton({ onIntent(HomeIntent.SignOut) }, enabled = !state.signingOut) { Text("Cerrar sesión") }
    }
}

@Composable
private fun AssignmentContent(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val assignments = state.content.assignments.filter {
        it.title.contains(state.search, true) && when (state.filter) {
            AssignmentFilter.All -> true
            AssignmentFilter.Pending -> it.status in listOf("pendiente", "en_curso")
            AssignmentFilter.Complete -> it.status == "completada"
            AssignmentFilter.Overdue -> it.status == "vencida"
        }
    }
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            OutlinedTextField(state.search, { onIntent(HomeIntent.Search(it)) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar asignaciones") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) })
        }
        item { Text("Asignaciones actuales", style = MaterialTheme.typography.titleLarge) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssignmentFilter.entries.forEach { filter ->
                    FilterChip(state.filter == filter, { onIntent(HomeIntent.Filter(filter)) }, label = { Text(filter.title) })
                }
            }
        }
        if (assignments.isEmpty() && !state.loading && state.failure == null) item {
            EmptyContent("Sin asignaciones", "Tus asignaciones aparecerán aquí cuando te incluyan en un equipo o coincidan con los filtros.")
        }
        items(assignments, key = { it.id }) { AssignmentCard(it) { onIntent(HomeIntent.OpenAssignment(it)) } }
    }
}

@Composable
private fun AssignmentCard(assignment: Assignment, onClick: () -> Unit) {
    Card(onClick, Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(assignment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(if (assignment.type == "campo") "Campo" else "Administrativa", color = MaterialTheme.colorScheme.primary)
            assignment.location?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            assignment.deadline?.let { Text(dateLabel(it), style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(statusLabel(assignment.status), style = MaterialTheme.typography.labelLarge)
                Text("Prioridad ${assignment.priority}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun BulletinsContent(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.content.bulletins.isEmpty() && !state.loading && state.failure == null) item {
            EmptyContent("Sin comunicados", "Los avisos de SounDisco aparecerán aquí.")
        }
        items(state.content.bulletins, key = { it.id }) { bulletin ->
            ListItem(
                headlineContent = { Text(bulletin.subject, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(bulletin.message, maxLines = 2) },
                modifier = Modifier.clickable { onIntent(HomeIntent.OpenBulletin(bulletin)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val datePicker = rememberDatePickerState(initialSelectedDateMillis = state.selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
    LaunchedEffect(datePicker.selectedDateMillis) {
        datePicker.selectedDateMillis?.let {
            onIntent(HomeIntent.SelectDate(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()))
        }
    }
    val entries = state.content.assignments.filter { dateOf(it.deadline) == state.selectedDate || it.scheduledDates.any { date -> dateOf(date) == state.selectedDate } }
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            DatePicker(datePicker, title = null, headline = null, showModeToggle = false)
        }
        item { Text("Agenda del día", style = MaterialTheme.typography.titleLarge) }
        if (entries.isEmpty() && !state.loading && state.failure == null) item { EmptyContent("Sin asignaciones", "No hay asignaciones con fecha para este día.") }
        items(entries, key = { it.id }) { AssignmentCard(it) { onIntent(HomeIntent.OpenAssignment(it)) } }
    }
}

@Composable
private fun ProfileContent(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(Modifier.widthIn(max = 680.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Icon(Icons.Outlined.AccountCircle, null, Modifier.size(80.dp))
        Text(state.profile?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall)
        listOf("Correo corporativo" to state.user.email, "Número de teléfono" to state.profile?.phone.orEmpty(),
            "Cargo" to state.profile?.position.orEmpty()).forEach { (label, value) ->
            ListItem(headlineContent = { Text(label) }, supportingContent = { Text(value) })
        }
        OutlinedButton({ onIntent(HomeIntent.SignOut) }, enabled = !state.signingOut, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.signingOut) "Cerrando sesión…" else "Cerrar sesión")
        }
    }
}

@Composable
private fun EmptyContent(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun dateOf(value: String?) = value?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDate() }.getOrNull() }

private fun dateLabel(value: String) = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es")))
}.getOrDefault(value)

private fun statusLabel(value: String) = when (value) {
    "completada" -> "Completada"
    "vencida" -> "Vencida"
    "en_curso" -> "En curso"
    else -> "Pendiente"
}
