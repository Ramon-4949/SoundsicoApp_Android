package edu.ucne.soundsicoappandroid.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.domain.model.EmployeePerformance
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AdminPerformanceScreen(
    values: List<EmployeePerformance>,
    loading: Boolean,
    error: String?,
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var employeeId by rememberSaveable { mutableStateOf<String?>(null) }
    val back = { if (employeeId != null) employeeId = null else onBack() }
    BackHandler(onBack = back)
    val employee = values.firstOrNull { it.id == employeeId }
    val roles = values.map { it.position }.distinct().sorted()
    val effectiveRole = role?.takeIf { it in roles }
    val visible = values.filter {
        (effectiveRole == null || it.position == effectiveRole) &&
            (it.name + " " + it.position).contains(search.trim(), true)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary) }
                Text(if (employeeId == null) "Rendimiento" else "Detalle de rendimiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onRetry, enabled = !loading) { Icon(Icons.Outlined.Refresh, "Actualizar") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton({ onMonth(month.minusMonths(1)) }) { Icon(Icons.Outlined.ChevronLeft, "Mes anterior") }
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-DO"))))
                IconButton({ onMonth(month.plusMonths(1)) }) { Icon(Icons.Outlined.ChevronRight, "Mes siguiente") }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message -> item {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onRetry) { Text("Reintentar") }
        } }
        if (employeeId == null) {
            item {
                OutlinedTextField(search, { search = it.take(100) }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Buscar por nombre o cargo") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(effectiveRole == null, { role = null }, label = { Text("Todos (${values.size})") }) }
                    items(roles) { position -> FilterChip(effectiveRole == position, { role = position }, label = { Text(position) }) }
                }
            }
            if (visible.isEmpty() && !loading && error == null) item { Text("No hay empleados que coincidan con la búsqueda.") }
            items(visible, key = { it.id }) { value ->
                Surface(Modifier.fillMaxWidth().clickable { employeeId = value.id }, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.AccountCircle, null, Modifier.size(40.dp))
                        Column(Modifier.weight(1f)) {
                            Text(value.name, fontWeight = FontWeight.Bold)
                            Text(value.position, style = MaterialTheme.typography.bodySmall)
                            Text("${value.assignments} asignaciones", style = MaterialTheme.typography.labelSmall)
                        }
                        ComplianceRing(value, true)
                    }
                }
            }
        } else if (employee != null && error == null) {
            item {
                MetricSection(employee.name) { Text(employee.position, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            item {
                MetricSection("Puntualidad de Hitos") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ComplianceRing(employee, false) }
                    MetricRow("Temprano", "${employee.early} hitos")
                    MetricRow("A tiempo", "${employee.onTime} hitos")
                    MetricRow("Tardío", "${employee.late} hitos")
                    MetricRow("Sin confirmar · plazo cerrado", "${employee.unconfirmed} hitos")
                    HorizontalDivider()
                    MetricRow("Retraso medio", employee.averageDelayMinutes?.let { String.format(Locale.US, "%.1f min", it) } ?: "—")
                }
            }
            item {
                MetricSection("Proactividad y Reportes") {
                    Text("${employee.weeklyNotes.sumOf { it.count }} notas e incidencias en el mes", style = MaterialTheme.typography.bodySmall)
                    val weeks = employee.weeklyNotes.sortedBy { it.week }
                    val maximum = (weeks.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
                    val highlighted = weeks.lastOrNull { it.count > 0 }?.week
                    if (weeks.isEmpty()) Text("Sin reportes semanales.")
                    else Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                        weeks.forEach { week ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(week.count.toString(), style = MaterialTheme.typography.labelSmall)
                                Box(Modifier.fillMaxWidth().height((100f * week.count / maximum).dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (week.week == highlighted) 1f else 0.22f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                Text("S${week.week}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                MetricSection("Carga de Trabajo Mensual") {
                    Text("${employee.assignments} asignaciones en el período")
                    Row(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                        if (employee.assignments > 0) {
                            val counts = listOf(employee.completed, employee.active, employee.overdue)
                            val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.outline)
                            counts.forEachIndexed { index, count ->
                                if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(colors[index]))
                            }
                            val remaining = employee.assignments - counts.sum()
                            if (remaining > 0) Spacer(Modifier.weight(remaining.toFloat()))
                        }
                    }
                    MetricRow("Completadas", employee.completed.toString())
                    MetricRow("Activas", employee.active.toString())
                    MetricRow("Vencidas", employee.overdue.toString())
                }
            }
        } else if (!loading && error == null) item { Text("El empleado ya no está disponible para consulta.") }
    }
}

@Composable
private fun ComplianceRing(value: EmployeePerformance, compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(if (compact) 46.dp else 116.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (value.compliance ?: 0.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(), strokeWidth = if (compact) 3.dp else 8.dp,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            Text(value.compliance?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
        }
        Text("Cumplimiento", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MetricSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
    }
}
