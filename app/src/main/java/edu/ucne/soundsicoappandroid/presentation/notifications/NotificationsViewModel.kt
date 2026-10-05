package edu.ucne.soundsicoappandroid.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class NotificationsState(
    val items: List<EmployeeNotification> = emptyList(),
    val loading: Boolean = true,
    val opening: Boolean = false,
    val failure: String? = null,
    val assignment: Assignment? = null,
    val bulletin: Bulletin? = null,
    val information: String? = null
) {
    val unread: Int get() = items.count { !it.read }
    val unreadBulletinIds: Set<String> get() {
        val deleted = items.filter { it.destinationType == "comunicado" && it.type == "comunicado_eliminado" }
            .mapNotNull { it.destinationId }.toSet()
        return items.filter {
            !it.read && it.destinationType == "comunicado" && it.type != "comunicado_eliminado"
        }.mapNotNull { it.destinationId }.toSet() - deleted
    }
    val unreadMessages: Int get() = unreadBulletinIds.size
}

class NotificationsViewModel(
    private val userId: String,
    private val repository: NotificationsRepository,
    private val assignments: AssignmentsRepository,
    private val bulletins: BulletinsRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(NotificationsState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { refresh() }

    fun refresh() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, failure = null) }
            repository.observeNotifications(userId)
                .retryWhen { error, _ ->
                    if (error is CancellationException) false else {
                        mutableState.update { it.copy(loading = false, failure = error.userMessage()) }
                        delay(30_000)
                        true
                    }
                }
                .catch { error -> mutableState.update { it.copy(loading = false, failure = error.userMessage()) } }
                .collect { items -> mutableState.update { it.copy(items = items, loading = false, failure = null) } }
        }
    }

    fun markRead(id: String? = null) {
        viewModelScope.launch {
            try {
                repository.markRead(id)
                mutableState.update { state -> state.copy(items = state.items.map {
                    if (id == null || id == it.id) it.copy(read = true) else it
                }) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            }
        }
    }

    fun markBulletinRead(bulletinId: String) {
        state.value.items.filter {
            !it.read && it.destinationType == "comunicado" && it.destinationId == bulletinId
        }.forEach { markRead(it.id) }
    }

    fun open(id: String, administrator: Boolean) {
        if (state.value.opening) return
        mutableState.update { it.copy(opening = true, failure = null, information = null) }
        viewModelScope.launch {
            try {
                val notification = repository.getNotifications(userId).firstOrNull { it.id == id }
                    ?: error("Esta notificación ya no está disponible.")
                repository.markRead(id)
                mutableState.update { current -> current.copy(items = current.items.map {
                    if (it.id == id) it.copy(read = true) else it
                }) }
                val destination = notification.destinationId
                if (destination == null || notification.type in listOf("asignacion_eliminada", "asignacion_retirada", "comunicado_eliminado")) {
                    mutableState.update { it.copy(information = notification.message ?: notification.title ?: "Sin detalles adicionales.") }
                } else when (notification.destinationType) {
                    "asignacion" -> {
                        val value = assignments.getAssignment(destination, userId.takeUnless { administrator })
                        mutableState.update { it.copy(assignment = value) }
                    }
                    "comunicado" -> {
                        val value = bulletins.getBulletin(destination)
                        mutableState.update { it.copy(bulletin = value) }
                    }
                    else -> mutableState.update { it.copy(information = notification.message ?: notification.title) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(failure = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(opening = false) }
            }
        }
    }

    fun clearDestination() { mutableState.update { it.copy(assignment = null, bulletin = null) } }
}
