package edu.ucne.soundsicoappandroid.presentation.assignmentdetail

import edu.ucne.soundsicoappandroid.domain.model.AssignmentDetails
import edu.ucne.soundsicoappandroid.domain.model.Milestone

data class AssignmentDetailState(
    val loading: Boolean = true,
    val details: AssignmentDetails? = null,
    val error: String? = null,
    val checkingMilestoneId: String? = null,
    val note: String = "",
    val savingNote: Boolean = false,
    val deleteConfirmation: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false
)

sealed interface AssignmentDetailIntent {
    data object Refresh : AssignmentDetailIntent
    data class CheckIn(val milestone: Milestone) : AssignmentDetailIntent
    data class ChangeNote(val value: String) : AssignmentDetailIntent
    data object AddNote : AssignmentDetailIntent
    data object RequestDelete : AssignmentDetailIntent
    data object CancelDelete : AssignmentDetailIntent
    data object ConfirmDelete : AssignmentDetailIntent
    data object DismissError : AssignmentDetailIntent
}
