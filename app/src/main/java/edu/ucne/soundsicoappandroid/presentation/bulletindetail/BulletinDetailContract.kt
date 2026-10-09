package edu.ucne.soundsicoappandroid.presentation.bulletindetail

import edu.ucne.soundsicoappandroid.domain.model.Bulletin

data class BulletinDetailState(
    val bulletin: Bulletin,
    val editing: Boolean = false,
    val subject: String = bulletin.subject,
    val message: String = bulletin.message,
    val saving: Boolean = false,
    val deleteConfirmation: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

sealed interface BulletinDetailIntent {
    data object Edit : BulletinDetailIntent
    data object CancelEdit : BulletinDetailIntent
    data class ChangeSubject(val value: String) : BulletinDetailIntent
    data class ChangeMessage(val value: String) : BulletinDetailIntent
    data object Save : BulletinDetailIntent
    data object RequestDelete : BulletinDetailIntent
    data object CancelDelete : BulletinDetailIntent
    data object ConfirmDelete : BulletinDetailIntent
    data object DismissError : BulletinDetailIntent
    data object DismissSuccess : BulletinDetailIntent
}
