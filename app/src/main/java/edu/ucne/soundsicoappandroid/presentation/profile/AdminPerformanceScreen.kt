package edu.ucne.soundsicoappandroid.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.domain.model.EmployeePerformance

@Composable
fun AdminPerformanceScreen(
    values: List<EmployeePerformance>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary) }
                Text("Rendimiento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onRetry, enabled = !loading) { Icon(Icons.Outlined.Refresh, "Actualizar") }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(onRetry) { Text("Reintentar") }
            }
        }
        if (!loading && error == null && values.isEmpty()) item {
            Text("No hay información de rendimiento para este mes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(values, key = EmployeePerformance::id) { performance ->
            PerformanceCard(performance)
        }
    }
}

@Composable
private fun PerformanceCard(value: EmployeePerformance) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Person, null) }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(value.name, fontWeight = FontWeight.Bold)
                    Text(value.position, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${value.completed}/${value.assignments}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceMetric("A tiempo", value.onTime, Color(0xFF2E9B55), Modifier.weight(1f))
                PerformanceMetric("Temprano", value.early, Color(0xFF2476C9), Modifier.weight(1f))
                PerformanceMetric("Tarde", value.late, Color(0xFFD47B18), Modifier.weight(1f))
                PerformanceMetric("Sin confirmar", value.unconfirmed, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
            value.averageDelayMinutes?.let {
                Text("Promedio de retraso: ${String.format(java.util.Locale.US, "%.1f", it)} min", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PerformanceMetric(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(9.dp), color = color.copy(alpha = 0.12f)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
