package edu.ucne.soundsicoappandroid.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.presentation.profile.AdminPerformanceScreen
import edu.ucne.soundsicoappandroid.presentation.profile.ProfileScreen

@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit, onCreateAssignment: () -> Unit = {}) {
    val approved = state.profile?.access == AccountAccess.Approved
    Scaffold(
        containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else Color(0xFFF9F9F9),
        bottomBar = {
            if (approved) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tab == tab,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
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
            if (approved && state.tab != HomeTab.Start && state.tab != HomeTab.Profile) {
                Row(Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(state.tab.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                HomeTab.Start -> if (state.audience == HomeAudience.Administrator) AdminDashboardView(state, onIntent, onCreateAssignment) else EmployeeAgendaView(state, onIntent)
                HomeTab.Messages -> BulletinsContent(state, onIntent)
                HomeTab.Calendar -> CalendarContent(state, onIntent)
                HomeTab.Profile -> state.profile?.let { profile ->
                    if (state.profilePage == ProfilePage.Performance && profile.isAdministrator) {
                        AdminPerformanceScreen(
                            state.performance,
                            state.performanceLoading,
                            state.performanceFailure,
                            onBack = { onIntent(HomeIntent.ClosePerformance) },
                            onRetry = { onIntent(HomeIntent.RetryPerformance) }
                        )
                    } else {
                        ProfileScreen(
                            profile,
                            state.user.email,
                            state.signingOut,
                            onOpenDashboard = { onIntent(HomeIntent.OpenDashboard) },
                            onOpenPerformance = { onIntent(HomeIntent.OpenPerformance) },
                            onSignOut = { onIntent(HomeIntent.SignOut) }
                        )
                    }
                }
            }
        }
    }
    PendingApprovalsDialog(state, onIntent)
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
    val assignments = if (state.audience == HomeAudience.Administrator) state.calendarAssignments.orEmpty() else state.content.assignments
    val entries = assignments.filter { it.occursOn(state.selectedDate) }
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            DatePicker(datePicker, title = null, headline = null, showModeToggle = false)
        }
        item { Text("Agenda del día", style = MaterialTheme.typography.titleLarge) }
        if (state.calendarLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.calendarFailure?.let { message -> item {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton({ onIntent(HomeIntent.SelectTab(HomeTab.Calendar)) }) { Text("Reintentar agenda") }
        } }
        if (entries.isEmpty() && !state.loading && !state.calendarLoading && state.failure == null && state.calendarFailure == null) item { EmptyContent("Sin asignaciones", "No hay asignaciones con fecha para este día.") }
        items(entries, key = { it.id }) { AssignmentCard(it) { onIntent(HomeIntent.OpenAssignment(it)) } }
    }
}

@Composable
private fun EmptyContent(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
