package edu.ucne.soundsicoappandroid.presentation.assignmentdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.model.Milestone
import edu.ucne.soundsicoappandroid.domain.repository.AdminRepository
import edu.ucne.soundsicoappandroid.domain.repository.MilestonesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AssignmentDetailViewModel(
    private val assignmentId: String,
    private val userId: String,
    private val administrator: Boolean,
    private val milestonesRepository: MilestonesRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssignmentDetailState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: AssignmentDetailIntent) {
        when (intent) {
            AssignmentDetailIntent.Refresh -> load()
            is AssignmentDetailIntent.CheckIn -> checkIn(intent.milestone)
            is AssignmentDetailIntent.ChangeNote -> mutableState.update { it.copy(note = intent.value.take(4000)) }
            AssignmentDetailIntent.AddNote -> addNote()
            AssignmentDetailIntent.RequestDelete -> if (administrator) mutableState.update { it.copy(deleteConfirmation = true) }
            AssignmentDetailIntent.CancelDelete -> mutableState.update { it.copy(deleteConfirmation = false) }
            AssignmentDetailIntent.ConfirmDelete -> delete()
            AssignmentDetailIntent.DismissError -> mutableState.update { it.copy(error = null) }
        }
    }

    private fun load() {
        if (state.value.loading && state.value.details != null) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val details = milestonesRepository.getDetails(assignmentId, userId)
                mutableState.update { it.copy(details = details) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    private fun checkIn(milestone: Milestone) {
        if (administrator || state.value.checkingMilestoneId != null) return
        mutableState.update { it.copy(checkingMilestoneId = milestone.id, error = null) }
        viewModelScope.launch {
            try {
                milestonesRepository.checkIn(milestone.id, userId)
                val details = milestonesRepository.getDetails(assignmentId, userId)
                mutableState.update { it.copy(details = details) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(checkingMilestoneId = null) }
            }
        }
    }

    private fun addNote() {
        val content = state.value.note.trim()
        if (content.length < 3 || state.value.savingNote) return
        mutableState.update { it.copy(savingNote = true, error = null) }
        viewModelScope.launch {
            try {
                milestonesRepository.addNote(UUID.randomUUID().toString(), assignmentId, content)
                val details = milestonesRepository.getDetails(assignmentId, userId)
                mutableState.update { it.copy(details = details, note = "") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(savingNote = false) }
            }
        }
    }

    private fun delete() {
        if (!administrator || state.value.deleting) return
        mutableState.update { it.copy(deleting = true, deleteConfirmation = false, error = null) }
        viewModelScope.launch {
            try {
                adminRepository.deleteAssignment(assignmentId)
                mutableState.update { it.copy(deleted = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(deleting = false) }
            }
        }
    }
}
