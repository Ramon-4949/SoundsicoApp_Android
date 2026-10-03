package edu.ucne.soundsicoappandroid.presentation.assignmentdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.domain.model.AssignmentCollaborator
import java.text.Normalizer

@Composable
internal fun AssignmentTeamSummary(members: List<AssignmentCollaborator>, onOpen: () -> Unit) {
    val team = members.distinctBy { it.id }
    val supervisors = team.filter { it.supervisor }
    val preview = supervisors.firstOrNull() ?: team.first()
    val remaining = team.filterNot { it.id == preview.id }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Outlined.Groups, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                Text("COLABORADORES ASIGNADOS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            TeamMemberCard(preview, compact = true)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                    remaining.take(2).forEach { TeamAvatar(it.name, 29) }
                    if (remaining.size > 2) Surface(
                        Modifier.size(29.dp), shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+${remaining.size - 2}", fontSize = 10.sp)
                        }
                    }
                }
                Text(
                    if (remaining.isEmpty()) "Equipo asignado" else "${remaining.size} colaboradores más",
                    Modifier.weight(1f).padding(start = 8.dp), fontSize = 11.sp
                )
                TextButton(onOpen, contentPadding = PaddingValues(horizontal = 9.dp)) {
                    Text("Ver lista", fontSize = 11.sp)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssignmentTeamScreen(members: List<AssignmentCollaborator>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    val team = members.distinctBy { it.id }
    val positions = team.mapNotNull { it.position?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted()
    val available = team.filter { it.matchesTeamSearch(search) }
    val selected = category.takeIf { it == "all" || it == "supervisors" || it.removePrefix("position:") in positions } ?: "all"
    val visible = available.filter {
        when (selected) {
            "all" -> true
            "supervisors" -> it.supervisor
            else -> it.position?.trim() == selected.removePrefix("position:")
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equipo asignado") },
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Buscar por nombre o especialidad", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (search.isNotEmpty()) IconButton({ search = "" }) { Icon(Icons.Outlined.Close, "Limpiar búsqueda") }
                    }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item {
                        FilterChip(selected == "all", { category = "all" }, label = { Text("Todos (${available.size})") })
                    }
                    item {
                        FilterChip(selected == "supervisors", { category = "supervisors" },
                            label = { Text("Supervisión (${available.count { it.supervisor }})") })
                    }
                    items(positions) { position ->
                        FilterChip(selected == "position:$position", { category = "position:$position" },
                            label = { Text("$position (${available.count { it.position?.trim() == position }})") })
                    }
                }
            }
            if (visible.isEmpty()) item {
                Text("No hay colaboradores que coincidan con los filtros.", Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val supervisors = visible.filter { it.supervisor }
            val collaborators = visible.filterNot { it.supervisor }
            if (supervisors.isNotEmpty()) {
                item { TeamSectionTitle("SUPERVISORES ASIGNADOS") }
                items(supervisors, key = { it.id }) { TeamMemberCard(it) }
            }
            if (collaborators.isNotEmpty()) {
                item { TeamSectionTitle("COLABORADORES ASIGNADOS") }
                items(collaborators, key = { it.id }) { TeamMemberCard(it) }
            }
        }
    }
}

@Composable
private fun TeamSectionTitle(title: String) {
    Text(title, Modifier.padding(top = 12.dp, bottom = 2.dp), fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.4.sp)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TeamMemberCard(member: AssignmentCollaborator, compact: Boolean = false) {
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        color = if (compact) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TeamAvatar(member.name, 40)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(member.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (member.supervisor) Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ) {
                        Text("Supervisor", Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (!member.position.isNullOrBlank()) Text(member.position, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TeamAvatar(name: String, size: Int) {
    Surface(Modifier.size(size.dp), shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
                fontSize = if (size > 30) 13.sp else 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

internal fun AssignmentCollaborator.matchesTeamSearch(search: String): Boolean {
    fun String.normalized() = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "").lowercase().trim()
    return (name + " " + position.orEmpty()).normalized().contains(search.normalized())
}
