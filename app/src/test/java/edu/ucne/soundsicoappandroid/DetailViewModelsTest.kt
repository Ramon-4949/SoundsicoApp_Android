package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.AdminRepository
import edu.ucne.soundsicoappandroid.domain.repository.BulletinsRepository
import edu.ucne.soundsicoappandroid.domain.repository.MilestonesRepository
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.AssignmentDetailIntent
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.AssignmentDetailViewModel
import edu.ucne.soundsicoappandroid.presentation.assignmentdetail.isConfirmed
import edu.ucne.soundsicoappandroid.domain.usecase.ConfirmMilestoneUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.ObserveAssignmentDetailsUseCase
import edu.ucne.soundsicoappandroid.domain.usecase.UndoMilestoneConfirmationUseCase
import edu.ucne.soundsicoappandroid.presentation.bulletindetail.BulletinDetailIntent
import edu.ucne.soundsicoappandroid.presentation.bulletindetail.BulletinDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun administratorCanDeleteAssignmentFromDetail() = runTest(dispatcher) {
        val admin = DetailAdminRepository()
        val milestones = DetailMilestonesRepository()
        val model = AssignmentDetailViewModel("assignment", "admin", true,
            ObserveAssignmentDetailsUseCase(milestones), ConfirmMilestoneUseCase(milestones),
            UndoMilestoneConfirmationUseCase(milestones), milestones, admin)
        advanceUntilIdle()

        assertEquals("Montaje", model.state.value.details?.assignment?.title)
        model.onIntent(AssignmentDetailIntent.RequestDelete)
        assertTrue(model.state.value.deleteConfirmation)
        model.onIntent(AssignmentDetailIntent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals("assignment", admin.deletedId)
        assertTrue(model.state.value.deleted)
    }

    @Test
    fun administratorCanEditAndDeleteBulletin() = runTest(dispatcher) {
        val repository = DetailBulletinsRepository()
        val model = BulletinDetailViewModel(Bulletin("bulletin", "Aviso", "Mensaje inicial"), true, repository)

        model.onIntent(BulletinDetailIntent.Edit)
        model.onIntent(BulletinDetailIntent.ChangeSubject("Aviso actualizado"))
        model.onIntent(BulletinDetailIntent.ChangeMessage("Contenido actualizado"))
        model.onIntent(BulletinDetailIntent.Save)
        advanceUntilIdle()

        assertEquals("Aviso actualizado", model.state.value.bulletin.subject)
        assertFalse(model.state.value.editing)
        model.onIntent(BulletinDetailIntent.RequestDelete)
        model.onIntent(BulletinDetailIntent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals("bulletin", repository.deletedId)
        assertTrue(model.state.value.deleted)
    }

    @Test
    fun checklistConfirmationReloadsTheCollaboratorsMilestone() = runTest(dispatcher) {
        val milestones = DetailMilestonesRepository()
        val model = AssignmentDetailViewModel("assignment", "employee", false,
            ObserveAssignmentDetailsUseCase(milestones), ConfirmMilestoneUseCase(milestones),
            UndoMilestoneConfirmationUseCase(milestones), milestones, DetailAdminRepository())
        advanceUntilIdle()
        val milestone = requireNotNull(model.state.value.details).assignment.milestones.single()

        assertFalse(milestone.isConfirmed(requireNotNull(model.state.value.details), "employee", false))
        model.onIntent(AssignmentDetailIntent.CheckIn(milestone))
        advanceUntilIdle()

        assertEquals("milestone", milestones.confirmedMilestoneId)
        assertTrue(milestone.isConfirmed(requireNotNull(model.state.value.details), "employee", false))
    }

    private class DetailMilestonesRepository : MilestonesRepository {
        var confirmedMilestoneId: String? = null
        private val assignment = Assignment(
            "assignment",
            "Montaje",
            "campo",
            "alta",
            "pendiente",
            "Auditorio",
            null,
            null,
            milestones = listOf(
                Milestone(
                    "milestone",
                    "assignment",
                    1,
                    "Llegada",
                    collaborators = listOf(MilestoneCollaborator("milestone", "employee", "asignado"))
                )
            )
        )

        override suspend fun getDetails(assignmentId: String, userId: String, administrator: Boolean) =
            AssignmentDetails(assignment, currentCheckIns(userId), emptyList(), emptyList())
        override suspend fun getCheckIns(assignmentId: String, userId: String) = emptyList<MilestoneCheckIn>()
        override suspend fun getNotes(assignmentId: String) = emptyList<AssignmentNote>()
        override suspend fun getCollaborators(assignmentId: String) = emptyList<AssignmentCollaborator>()
        override suspend fun checkIn(milestoneId: String, userId: String): MilestoneCheckIn {
            confirmedMilestoneId = milestoneId
            return MilestoneCheckIn("check-in", milestoneId, userId, "2026-10-02T12:00:00-04:00", "pendiente")
        }
        override suspend fun undoCheckIn(milestoneId: String) = Unit
        override suspend fun addNote(id: String, assignmentId: String, content: String) = Unit
        override fun observeDetails(assignmentId: String, userId: String, administrator: Boolean): Flow<AssignmentDetails> =
            flowOf(AssignmentDetails(assignment, emptyList(), emptyList(), emptyList()))

        private fun currentCheckIns(userId: String) = confirmedMilestoneId?.let {
            listOf(MilestoneCheckIn("check-in", it, userId, "2026-10-02T12:00:00-04:00", "pendiente"))
        }.orEmpty()
    }

    private class DetailAdminRepository : AdminRepository {
        var deletedId: String? = null
        override suspend fun getDashboardMetrics() = DashboardMetrics(0, 0, 0, 0.0, emptyList())
        override suspend fun getAssignments(offset: Int, limit: Int, filter: AdminAssignmentFilter, search: String) =
            AdminAssignmentPage(emptyList(), false, null)
        override suspend fun getEmployees() = emptyList<Employee>()
        override suspend fun createAssignment(draft: AssignmentDraft) = error("No disponible")
        override suspend fun updateAssignment(id: String, draft: AssignmentDraft) = error("No disponible")
        override suspend fun deleteAssignment(id: String) {
            deletedId = id
        }
        override suspend fun getAccounts() = emptyList<ManagedAccount>()
        override suspend fun reviewAccount(userId: String, state: AccountAccessState) = Unit
        override suspend fun getEmployeePerformance(month: String, employeeId: String?) = emptyList<EmployeePerformance>()
        override suspend fun getEmployeeAvailability(window: AssignmentBookingWindow, excludingAssignmentId: String?) =
            emptyList<EmployeeAvailability>()
    }

    private class DetailBulletinsRepository : BulletinsRepository {
        var deletedId: String? = null
        override suspend fun getBulletins() = emptyList<Bulletin>()
        override suspend fun getBulletin(id: String) = error("No disponible")
        override suspend fun createBulletin(subject: String, message: String) = Bulletin("created", subject, message)
        override suspend fun updateBulletin(id: String, subject: String, message: String) = Bulletin(id, subject, message)
        override suspend fun deleteBulletin(id: String) {
            deletedId = id
        }
    }
}
