package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.AdminRepository
import edu.ucne.soundsicoappandroid.domain.repository.BulletinsRepository
import edu.ucne.soundsicoappandroid.presentation.admincreation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AdminCreationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val admin = TestAdminRepository()
    private val bulletins = TestBulletinsRepository()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun fieldFlowBuildsCompleteAssignmentDraft() = runTest(dispatcher) {
        val model = AdminCreationViewModel(admin, bulletins)
        model.onIntent(AdminCreationIntent.SelectType(CreationType.Field))
        model.onIntent(AdminCreationIntent.Continue)
        advanceUntilIdle()
        assertEquals(CreationPage.Form, model.state.value.page)
        assertEquals(2, model.state.value.employees.size)

        val milestoneId = model.state.value.milestones.single().id
        model.onIntent(AdminCreationIntent.OpenMilestoneResponsibles(milestoneId))
        assertEquals(CreationPage.Responsibles, model.state.value.page)
        assertEquals(ResponsibleTarget.Milestone, model.state.value.responsibleTarget)
        model.onIntent(AdminCreationIntent.ToggleMilestoneEmployee(milestoneId, "employee"))
        model.onIntent(AdminCreationIntent.ConfirmResponsibles)
        model.onIntent(AdminCreationIntent.ChangeTitle("Montaje de sonido"))
        model.onIntent(AdminCreationIntent.ChangeLocation("Hotel Las Caobas"))
        model.onIntent(AdminCreationIntent.ChangeMilestoneTitle(milestoneId, "Llegada al almacén"))
        model.onIntent(AdminCreationIntent.AddMilestone)
        val secondMilestoneId = model.state.value.milestones.last().id
        model.onIntent(AdminCreationIntent.ChangeMilestoneTitle(secondMilestoneId, "Montaje técnico"))
        model.onIntent(AdminCreationIntent.OpenMilestoneResponsibles(secondMilestoneId))
        model.onIntent(AdminCreationIntent.ToggleMilestoneEmployee(secondMilestoneId, "employee-2"))
        model.onIntent(AdminCreationIntent.ConfirmResponsibles)
        model.onIntent(AdminCreationIntent.OpenSupervisorResponsibles)
        assertEquals(ResponsibleTarget.Supervisors, model.state.value.responsibleTarget)
        model.onIntent(AdminCreationIntent.ToggleSupervisor("employee"))
        model.onIntent(AdminCreationIntent.ConfirmResponsibles)
        model.onIntent(AdminCreationIntent.Submit)
        advanceUntilIdle()

        assertTrue(model.state.value.completed)
        assertEquals(AssignmentFlowType.Field, admin.created?.flowType)
        assertEquals(setOf("employee", "employee-2"), admin.created?.employeeIds?.toSet())
        assertEquals(listOf("employee"), admin.created?.milestones?.first()?.collaboratorIds)
        assertEquals(listOf("employee-2"), admin.created?.milestones?.last()?.collaboratorIds)
    }

    @Test
    fun messageFlowCreatesBulletin() = runTest(dispatcher) {
        val model = AdminCreationViewModel(admin, bulletins)
        model.onIntent(AdminCreationIntent.SelectType(CreationType.Message))
        model.onIntent(AdminCreationIntent.Continue)
        model.onIntent(AdminCreationIntent.ChangeSubject("Itinerario semanal"))
        model.onIntent(AdminCreationIntent.ChangeMessage("Reunión general a las nueve."))
        model.onIntent(AdminCreationIntent.Submit)
        advanceUntilIdle()

        assertTrue(model.state.value.completed)
        assertEquals("Itinerario semanal", bulletins.subject)
    }

    @Test
    fun editFlowPreservesMilestoneAssignmentsAndUpdatesExistingRecord() = runTest(dispatcher) {
        val assignment = Assignment(
            id = "assignment",
            title = "Montaje inicial",
            type = "campo",
            priority = "alta",
            status = "en_curso",
            location = "Auditorio",
            deadline = null,
            instructions = "Llevar equipos",
            milestones = listOf(
                Milestone(
                    id = "milestone",
                    assignmentId = "assignment",
                    order = 1,
                    title = "Llegada",
                    scheduledAt = "2026-10-04T09:00:00-04:00",
                    status = "en_curso",
                    collaborators = listOf(MilestoneCollaborator("milestone", "employee", "asignado"))
                )
            ),
            supervisors = listOf(AssignmentSupervisor("employee-2"))
        )
        val model = AdminCreationViewModel(admin, bulletins, assignment)
        advanceUntilIdle()

        assertTrue(model.state.value.editing)
        assertEquals(setOf("employee"), model.state.value.milestones.single().collaboratorIds)
        model.onIntent(AdminCreationIntent.ChangeTitle("Montaje actualizado"))
        model.onIntent(AdminCreationIntent.Submit)
        advanceUntilIdle()

        assertTrue(model.state.value.completed)
        assertEquals("assignment", admin.updatedId)
        assertEquals("Montaje actualizado", admin.updated?.title)
        assertEquals(MilestoneStatus.InProgress, admin.updated?.milestones?.single()?.status)
    }

    private class TestAdminRepository : AdminRepository {
        var created: AssignmentDraft? = null
        var updatedId: String? = null
        var updated: AssignmentDraft? = null
        override suspend fun getDashboardMetrics() = DashboardMetrics(0, 0, 0, 0.0, emptyList())
        override suspend fun getAssignments(offset: Int, limit: Int, filter: AdminAssignmentFilter, search: String) =
            AdminAssignmentPage(emptyList(), false, null)
        override suspend fun getEmployees() = listOf(
            Employee("employee", "Ana Pérez", "empleado", "Técnico"),
            Employee("employee-2", "Luis Reyes", "empleado", "Logística")
        )
        override suspend fun createAssignment(draft: AssignmentDraft): Assignment {
            created = draft
            return Assignment("created", draft.title, draft.flowType.value, draft.priority.value, "pendiente", draft.location, null, draft.instructions)
        }
        override suspend fun updateAssignment(id: String, draft: AssignmentDraft): Assignment {
            updatedId = id
            updated = draft
            return Assignment(id, draft.title, draft.flowType.value, draft.priority.value, draft.status.value, draft.location, null, draft.instructions)
        }
        override suspend fun deleteAssignment(id: String) = Unit
        override suspend fun getAccounts() = emptyList<ManagedAccount>()
        override suspend fun reviewAccount(userId: String, state: AccountAccessState) = Unit
        override suspend fun getEmployeePerformance(month: String, employeeId: String?) = emptyList<EmployeePerformance>()
        override suspend fun getEmployeeAvailability(window: AssignmentBookingWindow, excludingAssignmentId: String?) =
            emptyList<EmployeeAvailability>()
    }

    private class TestBulletinsRepository : BulletinsRepository {
        var subject: String? = null
        override suspend fun getBulletins() = emptyList<Bulletin>()
        override suspend fun getBulletin(id: String) = error("No disponible")
        override suspend fun createBulletin(subject: String, message: String): Bulletin {
            this.subject = subject
            return Bulletin("bulletin", subject, message)
        }
        override suspend fun updateBulletin(id: String, subject: String, message: String) = Bulletin(id, subject, message)
        override suspend fun deleteBulletin(id: String) = Unit
    }
}
