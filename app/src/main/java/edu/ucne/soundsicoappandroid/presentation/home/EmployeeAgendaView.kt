package edu.ucne.soundsicoappandroid.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmployeeAgendaView(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { HomeHeader(state, onIntent) }
        item { AssignmentSectionHeader(state, onIntent) }
        assignmentItems(state, onIntent)
    }
}
