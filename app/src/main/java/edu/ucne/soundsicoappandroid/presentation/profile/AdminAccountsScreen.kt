package edu.ucne.soundsicoappandroid.presentation.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.presentation.home.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AdminAccountsScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    BackHandler { onIntent(HomeIntent.ClosePerformance) }
    var search by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(AccountAccessState.Pending) }
    var reviewId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewState by rememberSaveable { mutableStateOf(AccountAccessState.Approved) }
    val visible = state.accounts.filter {
        it.state == selected && (it.name.orEmpty() + " " + it.email.orEmpty() + " " + it.position.orEmpty()).contains(search.trim(), true)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ onIntent(HomeIntent.ClosePerformance) }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
                Text("Accesos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton({ onIntent(HomeIntent.RetryAccounts) }, enabled = !state.accountsLoading) { Text("Actualizar") }
            }
        }
        item {
            OutlinedTextField(search, { search = it.take(100) }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Buscar por nombre, email o cargo") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(14.dp))
        }
        item {
            ScrollableTabRow(selected.ordinal, edgePadding = 0.dp) {
                AccountAccessState.entries.forEach { access ->
                    Tab(selected == access, { selected = access }, text = {
                        Text(when (access) {
                            AccountAccessState.Pending -> "Pendientes (${state.accounts.count { it.state == access }})"
                            AccountAccessState.Approved -> "Aprobadas"
                            AccountAccessState.Rejected -> "Rechazadas"
                        })
                    })
                }
            }
        }
        if (state.accountsLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.accountsFailure?.let { message -> item {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton({ onIntent(HomeIntent.RetryAccounts) }) { Text("Reintentar") }
        } }
        if (visible.isEmpty() && !state.accountsLoading && state.accountsFailure == null) item {
            Text("No hay cuentas que coincidan con estos filtros.", Modifier.padding(vertical = 24.dp))
        }
        items(visible, key = { it.id }) { account ->
            Surface(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.AccountCircle, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.outline)
                        Column {
                            Text(account.name ?: "Sin nombre", fontWeight = FontWeight.Bold)
                            Text(account.email.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(listOfNotNull(account.position, account.phone).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(runCatching { OffsetDateTime.parse(account.date).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")) }.getOrDefault(account.date),
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (account.state == AccountAccessState.Pending) OutlinedButton(
                            { reviewId = account.id; reviewState = AccountAccessState.Rejected },
                            Modifier.weight(1f), enabled = state.reviewingAccount == null
                        ) { Text("Rechazar") }
                        if (account.state != AccountAccessState.Approved) Button(
                            { onIntent(HomeIntent.ReviewManagedAccount(account.id, AccountAccessState.Approved)) },
                            Modifier.weight(1f), enabled = state.reviewingAccount == null
                        ) { Text("Aprobar acceso") }
                    }
                    if (state.reviewingAccount == account.id) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
    reviewId?.let { id ->
        AlertDialog(
            onDismissRequest = { reviewId = null },
            title = { Text(if (reviewState == AccountAccessState.Approved) "Aprobar acceso" else "Rechazar acceso") },
            text = { Text("Confirma el cambio de acceso para ${state.accounts.firstOrNull { it.id == id }?.name.orEmpty()}.") },
            dismissButton = { TextButton({ reviewId = null }) { Text("Cancelar") } },
            confirmButton = { TextButton({
                onIntent(HomeIntent.ReviewManagedAccount(id, reviewState))
                reviewId = null
            }) { Text("Confirmar") } }
        )
    }
}
