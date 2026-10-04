package edu.ucne.soundsicoappandroid.presentation.admincreation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.domain.model.AssignmentPriority
import edu.ucne.soundsicoappandroid.domain.model.Employee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreationScreen(
    state: AdminCreationState,
    onIntent: (AdminCreationIntent) -> Unit,
    onClose: () -> Unit,
    onCompleted: () -> Unit
) {
    BackHandler { if (state.page == CreationPage.Type) onClose() else onIntent(AdminCreationIntent.Back) }
    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (state.page) {
                        CreationPage.Form -> Text(
                            if (state.selectedType == CreationType.Message) "Crear Mensaje" else if (state.editing) "Editar Asignación" else "Crear Asignación",
                            fontWeight = FontWeight.SemiBold
                        )
                        CreationPage.Responsibles -> Text(
                            if (state.responsibleTarget == ResponsibleTarget.Supervisors) "Supervisores" else "Responsables",
                            fontWeight = FontWeight.SemiBold
                        )
                        CreationPage.Type -> Unit
                    }
                },
                navigationIcon = {
                    IconButton({ if (state.page == CreationPage.Type) onClose() else onIntent(AdminCreationIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.page == CreationPage.Type) CreationTypeContent(state, onIntent)
            else if (state.page == CreationPage.Responsibles) ResponsibleSelectionScreen(state, onIntent)
            else if (state.selectedType == CreationType.Message) MessageForm(state, onIntent)
            else AssignmentForm(state, onIntent)
        }
    }
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { onIntent(AdminCreationIntent.DismissError) },
            title = { Text("No se pudo continuar") },
            text = { Text(message) },
            confirmButton = {
                TextButton({ onIntent(AdminCreationIntent.DismissError) }) { Text("Entendido") }
            }
        )
    }
}

@Composable
private fun CreationTypeContent(state: AdminCreationState, onIntent: (AdminCreationIntent) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("¿Qué tipo de asignación\ndeseas crear?", fontSize = 29.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "Selecciona el entorno operativo para configurar automáticamente los flujos, métricas de ruta y parámetros requeridos.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 23.sp
        )
        CreationTypeCard(
            title = "Operaciones de Campo",
            label = "FLUJO OPERATIVO",
            description = "Montaje de equipos, logística, transporte, soporte técnico y eventos en locación externa.",
            icon = Icons.Outlined.LocalShipping,
            selected = state.selectedType == CreationType.Field
        ) { onIntent(AdminCreationIntent.SelectType(CreationType.Field)) }
        CreationTypeCard(
            title = "Tareas Administrativas",
            label = "GESTIÓN CORPORATIVA",
            description = "Elaboración de reportes, cotizaciones ejecutivas, auditoría de inventario y supervisión interna.",
            icon = Icons.AutoMirrored.Outlined.Assignment,
            selected = state.selectedType == CreationType.Administrative
        ) { onIntent(AdminCreationIntent.SelectType(CreationType.Administrative)) }
        CreationTypeCard(
            title = "Mensajes",
            label = "COMUNICADOS Y DISTRIBUCIÓN",
            description = "Envía mensajes, comunicados e itinerarios masivos a todos los colaboradores.",
            icon = Icons.Outlined.Forum,
            selected = state.selectedType == CreationType.Message
        ) { onIntent(AdminCreationIntent.SelectType(CreationType.Message)) }
        Spacer(Modifier.height(8.dp))
        Button(
            { onIntent(AdminCreationIntent.Continue) },
            enabled = state.selectedType != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continuar")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

@Composable
private fun CreationTypeCard(
    title: String,
    label: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f), shape = RoundedCornerShape(10.dp)) {
                    Icon(icon, null, Modifier.padding(11.dp).size(25.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                RadioButton(selected, onClick)
            }
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AssignmentForm(state: AdminCreationState, onIntent: (AdminCreationIntent) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FormLabel("TÍTULO DE LA ASIGNACIÓN")
            OutlinedTextField(
                state.title,
                { onIntent(AdminCreationIntent.ChangeTitle(it)) },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Montaje de Sonido Principal") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Assignment, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
        if (state.selectedType == CreationType.Field) item {
            FormLabel("UBICACIÓN DEL EVENTO")
            OutlinedTextField(
                state.location,
                { onIntent(AdminCreationIntent.ChangeLocation(it)) },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Auditorio Central - Piso 2") },
                leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
        item { FormLabel("ITINERARIO Y LOGÍSTICA DE TIEMPOS") }
        items(state.milestones, key = { it.id }) { milestone ->
            MilestoneEditor(
                milestone,
                state.milestones.size > 1,
                onSelectCollaborators = { onIntent(AdminCreationIntent.OpenMilestoneResponsibles(milestone.id)) },
                onIntent = onIntent
            )
        }
        item {
            OutlinedButton(
                { onIntent(AdminCreationIntent.AddMilestone) },
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.AddCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Añadir Hito / Horario")
            }
        }
        item {
            FormLabel("SUPERVISOR")
            SelectionField(
                icon = Icons.Outlined.SupervisorAccount,
                text = if (state.supervisorIds.isEmpty()) "Seleccionar supervisores" else "${state.supervisorIds.size} supervisores asignados",
                loading = state.loadingEmployees
            ) { onIntent(AdminCreationIntent.OpenSupervisorResponsibles) }
        }
        item {
            FormLabel("NIVEL DE PRIORIDAD")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssignmentPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = state.priority == priority,
                        onClick = { onIntent(AdminCreationIntent.ChangePriority(priority)) },
                        label = { Text(priority.label()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp)
                    )
                }
            }
        }
        item {
            FormLabel("INSTRUCCIONES LOGÍSTICAS (OPCIONAL)")
            OutlinedTextField(
                state.instructions,
                { onIntent(AdminCreationIntent.ChangeInstructions(it)) },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Revisar balance de cableado y consolas auxiliares.") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )
        }
        item {
            Button(
                { onIntent(AdminCreationIntent.Submit) },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else {
                    Icon(Icons.Outlined.TaskAlt, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (state.editing) "Guardar cambios" else "Crear asignación")
                }
            }
        }
    }
}

@Composable
private fun MilestoneEditor(
    milestone: MilestoneInput,
    removable: Boolean,
    onSelectCollaborators: () -> Unit,
    onIntent: (AdminCreationIntent) -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    milestone.title,
                    { onIntent(AdminCreationIntent.ChangeMilestoneTitle(milestone.id, it)) },
                    Modifier.weight(1f),
                    placeholder = { Text("Salida en ruta") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(9.dp)
                )
                if (removable) IconButton({ onIntent(AdminCreationIntent.RemoveMilestone(milestone.id)) }) {
                    Icon(Icons.Outlined.Close, "Eliminar hito")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    milestone.date,
                    { onIntent(AdminCreationIntent.ChangeMilestoneDate(milestone.id, it)) },
                    Modifier.weight(1f),
                    label = { Text("FECHA", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(9.dp)
                )
                OutlinedTextField(
                    milestone.time,
                    { onIntent(AdminCreationIntent.ChangeMilestoneTime(milestone.id, it)) },
                    Modifier.weight(1f),
                    label = { Text("HORARIO", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(9.dp)
                )
            }
            SelectionField(
                icon = Icons.Outlined.Group,
                text = if (milestone.collaboratorIds.isEmpty()) "Colaboradores Asignados" else "Colaboradores Asignados (${milestone.collaboratorIds.size})",
                loading = false,
                onClick = onSelectCollaborators
            )
        }
    }
}

@Composable
private fun MessageForm(state: AdminCreationState, onIntent: (AdminCreationIntent) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            FormLabel("ASUNTO DEL COMUNICADO")
            OutlinedTextField(
                state.subject,
                { onIntent(AdminCreationIntent.ChangeSubject(it)) },
                Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary) },
                placeholder = { Text("Asunto del mensaje") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }
        Column {
            FormLabel("MENSAJE / INSTRUCCIONES")
            OutlinedTextField(
                state.message,
                { onIntent(AdminCreationIntent.ChangeMessage(it)) },
                Modifier.fillMaxWidth().heightIn(min = 190.dp),
                placeholder = { Text("Escribe el comunicado para los colaboradores") },
                supportingText = { Text("${state.message.length} caracteres", Modifier.fillMaxWidth()) },
                shape = RoundedCornerShape(12.dp)
            )
        }
        Button(
            { onIntent(AdminCreationIntent.Submit) },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else {
                Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Crear Mensaje")
            }
        }
    }
}

@Composable
private fun SelectionField(icon: ImageVector, text: String, loading: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(enabled = !loading, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Text(if (loading) "Cargando personal…" else text, Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

@Composable
private fun ResponsibleSelectionScreen(state: AdminCreationState, onIntent: (AdminCreationIntent) -> Unit) {
    val milestone = state.milestones.firstOrNull { it.id == state.responsibleMilestoneId }
    val selected = if (state.responsibleTarget == ResponsibleTarget.Supervisors) state.supervisorIds
    else milestone?.collaboratorIds.orEmpty()
    val categories = remember(state.employees) {
        listOf("Todos") + state.employees.mapNotNull { it.position?.takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val filtered = state.employees.filter { employee ->
        val matchesSearch = state.responsibleSearch.isBlank() ||
            "${employee.name} ${employee.position.orEmpty()}".contains(state.responsibleSearch.trim(), true)
        val matchesCategory = state.responsibleCategory == "Todos" || employee.position == state.responsibleCategory
        matchesSearch && matchesCategory
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                state.responsibleSearch,
                { onIntent(AdminCreationIntent.ChangeResponsibleSearch(it)) },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre o cargo") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (state.responsibleSearch.isNotEmpty()) IconButton({ onIntent(AdminCreationIntent.ChangeResponsibleSearch("")) }) {
                        Icon(Icons.Outlined.Cancel, "Limpiar búsqueda")
                    }
                },
                singleLine = true,
                shape = CircleShape
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it }) { category ->
                    FilterChip(
                        selected = state.responsibleCategory == category,
                        onClick = { onIntent(AdminCreationIntent.ChangeResponsibleCategory(category)) },
                        label = { Text(if (category == "Todos") "Todos (${state.employees.size})" else category) },
                        shape = CircleShape
                    )
                }
            }
            if (selected.isNotEmpty()) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (state.responsibleTarget == ResponsibleTarget.Supervisors) "SELECCIONADOS PARA ESTA ASIGNACIÓN"
                            else "SELECCIONADOS PARA ESTE HITO",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(state.employees.filter { it.id in selected }, key = { it.id }) { employee ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        if (state.responsibleTarget == ResponsibleTarget.Supervisors)
                                            onIntent(AdminCreationIntent.ToggleSupervisor(employee.id))
                                        else state.responsibleMilestoneId?.let {
                                            onIntent(AdminCreationIntent.ToggleMilestoneEmployee(it, employee.id))
                                        }
                                    },
                                    label = { Text(employee.name.ifBlank { "Sin nombre" }) },
                                    avatar = { ResponsibleAvatar(employee, Modifier.size(24.dp)) },
                                    trailingIcon = { Icon(Icons.Outlined.Cancel, "Quitar", Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            if (state.responsibleTarget == ResponsibleTarget.Supervisors) "SUPERVISORES DISPONIBLES (${filtered.size})"
            else "COLABORADORES DISPONIBLES (${filtered.size})",
            Modifier.padding(start = 18.dp, top = 16.dp, bottom = 8.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(filtered, key = { it.id }) { employee ->
                ResponsibleEmployeeCard(
                    employee = employee,
                    selected = employee.id in selected,
                    onClick = {
                        if (state.responsibleTarget == ResponsibleTarget.Supervisors)
                            onIntent(AdminCreationIntent.ToggleSupervisor(employee.id))
                        else state.responsibleMilestoneId?.let {
                            onIntent(AdminCreationIntent.ToggleMilestoneEmployee(it, employee.id))
                        }
                    }
                )
            }
        }
        Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
            Button(
                { onIntent(AdminCreationIntent.ConfirmResponsibles) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text("Confirmar Selección (${selected.size})")
            }
        }
    }
}

@Composable
private fun ResponsibleEmployeeCard(employee: Employee, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ResponsibleAvatar(employee, Modifier.size(46.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    employee.name.ifBlank { "Empleado sin nombre" },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        employee.position ?: employee.role,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(color = Color(0xFFD6F7EA), shape = RoundedCornerShape(4.dp)) {
                        Text("Disponible", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF007A52), fontSize = 9.sp)
                    }
                }
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            else Icon(Icons.Outlined.RadioButtonUnchecked, "No seleccionado", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun ResponsibleAvatar(employee: Employee, modifier: Modifier = Modifier) {
    val initials = employee.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    Surface(modifier, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Box(contentAlignment = Alignment.Center) {
            Text(initials.ifBlank { "?" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FormLabel(value: String) {
    Text(value, fontSize = 10.sp, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(5.dp))
}

private fun AssignmentPriority.label() = when (this) {
    AssignmentPriority.Low -> "Baja"
    AssignmentPriority.Medium -> "Media"
    AssignmentPriority.High -> "Alta"
}
