package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.*
import edu.ucne.soundsicoappandroid.presentation.notifications.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val item = EmployeeNotification("notice", "Montaje", "Actualización", false, "2026-10-03T12:00:00Z",
        "asignacion_actualizada", "assignment", "asignacion", "pendiente")
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun realtimeChangesUpdateUnreadCount() = runTest(dispatcher) {
        val repository = Notices(item)
        val model = NotificationsViewModel("employee", repository, Assignments(), Bulletins())
        advanceUntilIdle()
        assertEquals(1, model.state.value.unread)
        repository.live.value = listOf(item.copy(read = true))
        advanceUntilIdle()
        assertEquals(0, model.state.value.unread)
    }

    @Test fun deletedAssignmentIsInformationalAndNeverFetched() = runTest(dispatcher) {
        val repository = Notices(item.copy(type = "asignacion_eliminada"))
        val assignments = Assignments()
        val model = NotificationsViewModel("employee", repository, assignments, Bulletins())
        advanceUntilIdle()
        model.open("notice", false)
        advanceUntilIdle()
        assertEquals(0, assignments.loads)
        assertEquals("Actualización", model.state.value.information)
        assertEquals(0, model.state.value.unread)
    }

    @Test fun foreignNotificationCannotOpenDestination() = runTest(dispatcher) {
        val assignments = Assignments()
        val model = NotificationsViewModel("employee", Notices(item), assignments, Bulletins())
        advanceUntilIdle()
        model.open("foreign", false)
        advanceUntilIdle()
        assertEquals(0, assignments.loads)
        assertNotNull(model.state.value.failure)
    }

    @Test fun employeeDestinationPreservesViewerIdentity() = runTest(dispatcher) {
        val assignments = Assignments()
        val model = NotificationsViewModel("employee", Notices(item), assignments, Bulletins())
        advanceUntilIdle()
        model.open("notice", false)
        advanceUntilIdle()
        assertEquals("employee", assignments.viewer)
        assertEquals("assignment", model.state.value.assignment?.id)
    }

    @Test fun filtersMatchIosCategoriesAndOperationalStates() {
        assertTrue(NotificationFilter.Unread.matches(item))
        assertTrue(NotificationFilter.Assignments.matches(item))
        assertFalse(NotificationFilter.Bulletins.matches(item))
        assertTrue(NotificationFilter.Overdue.matches(item.copy(status = "vencida")))
    }

    @Test fun messageBadgeCountsUnreadBulletinsOnlyOnce() {
        val bulletin = item.copy(type = "comunicado_nuevo", destinationId = "bulletin", destinationType = "comunicado")
        val state = NotificationsState(items = listOf(bulletin, bulletin.copy(id = "second"), item))

        assertEquals(setOf("bulletin"), state.unreadBulletinIds)
        assertEquals(1, state.unreadMessages)
    }

    private class Notices(item: EmployeeNotification) : NotificationsRepository {
        val live = MutableStateFlow(listOf(item))
        override suspend fun getNotifications(userId: String) = live.value
        override suspend fun markRead(id: String?) { live.value = live.value.map { if (id == null || it.id == id) it.copy(read = true) else it } }
        override fun observeNotifications(userId: String) = live
    }
    private class Assignments : AssignmentsRepository {
        var loads = 0
        var viewer: String? = null
        override suspend fun getAssignments(userId: String) = emptyList<Assignment>()
        override suspend fun getAssignment(id: String, viewerId: String?): Assignment {
            loads++
            viewer = viewerId
            return Assignment(id, "Montaje", "campo", "alta", "pendiente", null, null, null)
        }
        override fun observeAssignments(userId: String) = emptyFlow<List<Assignment>>()
    }
    private class Bulletins : BulletinsRepository {
        override suspend fun getBulletins() = emptyList<Bulletin>()
        override suspend fun getBulletin(id: String) = Bulletin(id, "Aviso", "Texto")
        override suspend fun createBulletin(subject: String, message: String) = Bulletin("new", subject, message)
        override suspend fun updateBulletin(id: String, subject: String, message: String) = Bulletin(id, subject, message)
        override suspend fun deleteBulletin(id: String) = Unit
    }
}
