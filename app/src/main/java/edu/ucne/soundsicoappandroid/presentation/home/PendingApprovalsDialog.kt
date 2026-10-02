package edu.ucne.soundsicoappandroid.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PendingApprovalsDialog(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    if (state.approvalsOpen && state.audience == HomeAudience.Administrator) AlertDialog(
        onDismissRequest = { onIntent(HomeIntent.CloseApprovals) },
        title = { Text("Aprobaciones pendientes") },
        text = {
            val accounts = state.content.dashboard?.pendingAccounts.orEmpty()
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (accounts.isEmpty()) item { Text("No hay cuentas pendientes de aprobación.") }
                items(accounts, key = { it.id }) { account ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(account.name, fontWeight = FontWeight.Bold)
                        Text(account.email)
                        Text(account.position)
                        Row {
                            TextButton({ onIntent(HomeIntent.RequestReview(account, false)) }, enabled = !state.savingAccount) { Text("Rechazar") }
                            Button({ onIntent(HomeIntent.RequestReview(account, true)) }, enabled = !state.savingAccount) { Text("Aprobar") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ onIntent(HomeIntent.CloseApprovals) }, enabled = !state.savingAccount) { Text("Cerrar") } }
    )
    state.review?.let { review ->
        AlertDialog(
            onDismissRequest = { onIntent(HomeIntent.CancelReview) },
            title = { Text(if (review.approved) "Aprobar cuenta" else "Rechazar cuenta") },
            text = { Text("¿Confirmas ${if (review.approved) "aprobar" else "rechazar"} el acceso de ${review.account.name}?") },
            confirmButton = { TextButton({ onIntent(HomeIntent.ConfirmReview) }, enabled = !state.savingAccount) {
                Text(if (state.savingAccount) "Guardando…" else "Confirmar")
            } },
            dismissButton = { TextButton({ onIntent(HomeIntent.CancelReview) }, enabled = !state.savingAccount) { Text("Cancelar") } }
        )
    }
    state.actionFailure?.let { message ->
        AlertDialog(onDismissRequest = { onIntent(HomeIntent.DismissActionError) }, title = { Text("No se pudo guardar") },
            text = { Text(message) }, confirmButton = { TextButton({ onIntent(HomeIntent.DismissActionError) }) { Text("Entendido") } })
    }
}
