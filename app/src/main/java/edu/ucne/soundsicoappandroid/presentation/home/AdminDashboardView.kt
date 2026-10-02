package edu.ucne.soundsicoappandroid.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.domain.model.DashboardMetrics
import java.util.Locale

@Composable
fun AdminDashboardView(state: HomeState, onIntent: (HomeIntent) -> Unit, onCreateAssignment: () -> Unit) {
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { HomeHeader(state, onIntent) }
        state.content.dashboard?.let { dashboard ->
            item { DashboardMetricCards(dashboard.metrics) }
            item {
                Button(onCreateAssignment, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Outlined.AddCircle, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Nueva Asignación")
                }
            }
        }
        item { AssignmentSectionHeader(state, onIntent) }
        assignmentItems(state, onIntent)
        if (state.content.nextOffset != null) item {
            OutlinedButton({ onIntent(HomeIntent.LoadMore) }, enabled = !state.loadingMore && !state.listLoading,
                modifier = Modifier.fillMaxWidth()) {
                if (state.loadingMore) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Cargar más asignaciones")
            }
        }
    }
}

@Composable
private fun DashboardMetricCards(metrics: DashboardMetrics) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Card(Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AssignmentTurnedIn, null, Modifier.size(24.dp))
                    Text("+${metrics.createdLastSevenDays.lastOrNull() ?: 0} Hoy", fontSize = 10.sp)
                }
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(metrics.assigned.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("unidades", fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text("Asignadas", fontSize = 12.sp)
                }
                val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                    val values = metrics.createdLastSevenDays
                    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
                    values.zipWithNext().forEachIndexed { index, (a, b) ->
                        val step = size.width / (values.size - 1).coerceAtLeast(1)
                        drawLine(color, Offset(step * index, size.height * (1f - a.toFloat() / max)),
                            Offset(step * (index + 1), size.height * (1f - b.toFloat() / max)), strokeWidth = 2.dp.toPx())
                    }
                }
            }
        }
        Card(Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, null, Modifier.size(24.dp))
                    Text("● Activo", fontSize = 10.sp)
                }
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(metrics.active.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("en curso", fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text("En progreso", fontSize = 12.sp)
                }
                Row(Modifier.heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tasa de entrega", fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text(String.format(Locale.forLanguageTag("es"), "%.1f%%", metrics.deliveryRate), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
