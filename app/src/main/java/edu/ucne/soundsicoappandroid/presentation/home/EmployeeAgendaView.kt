package edu.ucne.soundsicoappandroid.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmployeeAgendaView(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.assignmentList.offset, state.filter) { listState.scrollToItem(0) }
    LazyColumn(Modifier.widthIn(max = 680.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { HomeHeader(state, onIntent) }
        item { AssignmentSectionHeader(state, onIntent) }
        assignmentItems(state, onIntent)
    }
}
