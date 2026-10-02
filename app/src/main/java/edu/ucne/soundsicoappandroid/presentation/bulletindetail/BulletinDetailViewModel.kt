package edu.ucne.soundsicoappandroid.presentation.bulletindetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.model.Bulletin
import edu.ucne.soundsicoappandroid.domain.repository.BulletinsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BulletinDetailViewModel(
    bulletin: Bulletin,
    private val administrator: Boolean,
    private val repository: BulletinsRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(BulletinDetailState(bulletin))
    val state = mutableState.asStateFlow()

    fun onIntent(intent: BulletinDetailIntent) {
        when (intent) {
            BulletinDetailIntent.Edit -> if (administrator) mutableState.update { it.copy(editing = true, error = null) }
            BulletinDetailIntent.CancelEdit -> mutableState.update {
                it.copy(editing = false, subject = it.bulletin.subject, message = it.bulletin.message, error = null)
            }
            is BulletinDetailIntent.ChangeSubject -> mutableState.update { it.copy(subject = intent.value.take(160)) }
            is BulletinDetailIntent.ChangeMessage -> mutableState.update { it.copy(message = intent.value.take(4000)) }
            BulletinDetailIntent.Save -> save()
            BulletinDetailIntent.RequestDelete -> if (administrator) mutableState.update { it.copy(deleteConfirmation = true) }
            BulletinDetailIntent.CancelDelete -> mutableState.update { it.copy(deleteConfirmation = false) }
            BulletinDetailIntent.ConfirmDelete -> delete()
            BulletinDetailIntent.DismissError -> mutableState.update { it.copy(error = null) }
        }
    }

    private fun save() {
        val input = state.value
        if (!administrator || input.saving) return
        val subject = input.subject.trim()
        val message = input.message.trim()
        val validation = when {
            subject.length < 3 -> "Escribe el asunto del comunicado."
            message.length < 3 -> "Escribe el contenido del comunicado."
            else -> null
        }
        if (validation != null) {
            mutableState.update { it.copy(error = validation) }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val updated = repository.updateBulletin(input.bulletin.id, subject, message)
                mutableState.update {
                    it.copy(bulletin = updated, subject = updated.subject, message = updated.message, editing = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    private fun delete() {
        if (!administrator || state.value.deleting) return
        mutableState.update { it.copy(deleting = true, deleteConfirmation = false, error = null) }
        viewModelScope.launch {
            try {
                repository.deleteBulletin(state.value.bulletin.id)
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
