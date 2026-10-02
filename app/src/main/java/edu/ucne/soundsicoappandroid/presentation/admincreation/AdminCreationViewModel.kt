package edu.ucne.soundsicoappandroid.presentation.admincreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.core.presentation.userMessage
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.domain.repository.AdminRepository
import edu.ucne.soundsicoappandroid.domain.repository.BulletinsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AdminCreationViewModel(
    private val adminRepository: AdminRepository,
    private val bulletinsRepository: BulletinsRepository,
    private val editingAssignment: Assignment? = null
) : ViewModel() {
    private val mutableState = MutableStateFlow(editingAssignment?.toCreationState() ?: AdminCreationState())
    val state = mutableState.asStateFlow()

    init {
        if (editingAssignment != null) loadEmployees()
    }

    fun onIntent(intent: AdminCreationIntent) {
        when (intent) {
            is AdminCreationIntent.SelectType -> mutableState.update { it.copy(selectedType = intent.value, error = null) }
            AdminCreationIntent.Continue -> continueToForm()
            AdminCreationIntent.Back -> mutableState.update {
                it.copy(page = if (it.page == CreationPage.Responsibles) CreationPage.Form else CreationPage.Type, error = null)
            }
            is AdminCreationIntent.ChangeTitle -> mutableState.update { it.copy(title = intent.value.take(120)) }
            is AdminCreationIntent.ChangeLocation -> mutableState.update { it.copy(location = intent.value.take(180)) }
            is AdminCreationIntent.ChangePriority -> mutableState.update { it.copy(priority = intent.value) }
            is AdminCreationIntent.ChangeInstructions -> mutableState.update { it.copy(instructions = intent.value.take(2000)) }
            is AdminCreationIntent.ToggleMilestoneEmployee -> updateMilestone(intent.milestoneId) {
                it.copy(collaboratorIds = it.collaboratorIds.toggle(intent.employeeId))
            }
            is AdminCreationIntent.ToggleSupervisor -> mutableState.update {
                it.copy(supervisorIds = it.supervisorIds.toggle(intent.id))
            }
            is AdminCreationIntent.OpenMilestoneResponsibles -> mutableState.update {
                it.copy(
                    page = CreationPage.Responsibles,
                    responsibleTarget = ResponsibleTarget.Milestone,
                    responsibleMilestoneId = intent.milestoneId,
                    responsibleSearch = "",
                    responsibleCategory = "Todos"
                )
            }
            AdminCreationIntent.OpenSupervisorResponsibles -> mutableState.update {
                it.copy(
                    page = CreationPage.Responsibles,
                    responsibleTarget = ResponsibleTarget.Supervisors,
                    responsibleMilestoneId = null,
                    responsibleSearch = "",
                    responsibleCategory = "Todos"
                )
            }
            is AdminCreationIntent.ChangeResponsibleSearch -> mutableState.update {
                it.copy(responsibleSearch = intent.value.take(80))
            }
            is AdminCreationIntent.ChangeResponsibleCategory -> mutableState.update {
                it.copy(responsibleCategory = intent.value)
            }
            AdminCreationIntent.ConfirmResponsibles -> mutableState.update {
                it.copy(page = CreationPage.Form, responsibleSearch = "", responsibleCategory = "Todos")
            }
            is AdminCreationIntent.ChangeMilestoneTitle -> updateMilestone(intent.id) { it.copy(title = intent.value.take(100)) }
            is AdminCreationIntent.ChangeMilestoneDate -> updateMilestone(intent.id) { it.copy(date = intent.value.take(10)) }
            is AdminCreationIntent.ChangeMilestoneTime -> updateMilestone(intent.id) { it.copy(time = intent.value.take(5)) }
            AdminCreationIntent.AddMilestone -> mutableState.update {
                it.copy(milestones = it.milestones + MilestoneInput(time = LocalTime.of(9, 0).plusHours(it.milestones.size.toLong()).toString()))
            }
            is AdminCreationIntent.RemoveMilestone -> mutableState.update {
                it.copy(milestones = it.milestones.filterNot { milestone -> milestone.id == intent.id }.ifEmpty { listOf(MilestoneInput()) })
            }
            is AdminCreationIntent.ChangeSubject -> mutableState.update { it.copy(subject = intent.value.take(160)) }
            is AdminCreationIntent.ChangeMessage -> mutableState.update { it.copy(message = intent.value.take(4000)) }
            AdminCreationIntent.Submit -> submit()
            AdminCreationIntent.DismissError -> mutableState.update { it.copy(error = null) }
        }
    }

    private fun continueToForm() {
        if (state.value.selectedType == null) {
            mutableState.update { it.copy(error = "Selecciona el tipo de contenido que deseas crear.") }
            return
        }
        mutableState.update { it.copy(page = CreationPage.Form, error = null) }
        if (state.value.selectedType != CreationType.Message && state.value.employees.isEmpty()) loadEmployees()
    }

    private fun loadEmployees() {
        mutableState.update { it.copy(loadingEmployees = true) }
        viewModelScope.launch {
            try {
                val employees = adminRepository.getEmployees()
                mutableState.update { it.copy(employees = employees) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(loadingEmployees = false) }
            }
        }
    }

    private fun submit() {
        val input = state.value
        if (input.saving) return
        val validation = validate(input)
        if (validation != null) {
            mutableState.update { it.copy(error = validation) }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                if (input.selectedType == CreationType.Message) {
                    bulletinsRepository.createBulletin(input.subject, input.message)
                } else {
                    if (editingAssignment == null) adminRepository.createAssignment(input.toDraft())
                    else adminRepository.updateAssignment(editingAssignment.id, input.toDraft())
                }
                mutableState.update { it.copy(completed = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(error = error.userMessage()) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    private fun validate(input: AdminCreationState): String? {
        if (input.selectedType == CreationType.Message) {
            if (input.subject.trim().length < 3) return "Escribe el asunto del comunicado."
            if (input.message.trim().length < 3) return "Escribe el contenido del comunicado."
            return null
        }
        if (input.title.trim().length < 3) return "Escribe el título de la asignación."
        if (input.selectedType == CreationType.Field && input.location.trim().length < 3) return "Escribe la ubicación del evento."
        input.milestones.forEach {
            if (it.title.trim().length < 2) return "Todos los hitos necesitan un título."
            if (it.collaboratorIds.isEmpty()) return "Selecciona colaboradores para cada hito."
            if (runCatching { LocalDate.parse(it.date) }.isFailure) return "Revisa las fechas. Usa el formato AAAA-MM-DD."
            if (runCatching { LocalTime.parse(it.time) }.isFailure) return "Revisa los horarios. Usa el formato HH:MM."
        }
        return null
    }

    private fun AdminCreationState.toDraft(): AssignmentDraft {
        val zone = ZoneId.of("America/Santo_Domingo")
        val createdAt = ZonedDateTime.now(zone).toOffsetDateTime().toString()
        val drafts = milestones.mapIndexed { index, milestone ->
            val scheduled = ZonedDateTime.of(LocalDate.parse(milestone.date), LocalTime.parse(milestone.time), zone)
                .toOffsetDateTime().toString()
            MilestoneDraft(
                id = milestone.id,
                collaboratorIds = milestone.collaboratorIds.toList(),
                order = index + 1,
                title = milestone.title,
                estimatedTime = LocalTime.parse(milestone.time).withSecond(0).toString() + ":00",
                scheduledAt = scheduled,
                status = milestone.status ?: if (index == 0) MilestoneStatus.InProgress else MilestoneStatus.Blocked
            )
        }
        return AssignmentDraft(
            id = editingAssignment?.id,
            supervisorIds = supervisorIds.toList(),
            flowType = if (selectedType == CreationType.Field) AssignmentFlowType.Field else AssignmentFlowType.Administrative,
            title = title,
            location = location.trim().takeIf { selectedType == CreationType.Field },
            priority = priority,
            instructions = instructions.trim().takeIf(String::isNotEmpty),
            employeeIds = milestones.flatMap { it.collaboratorIds }.distinct(),
            status = AssignmentStatus.entries.firstOrNull { it.value == editingAssignment?.status } ?: AssignmentStatus.Pending,
            createdAt = editingAssignment?.createdAt ?: createdAt,
            milestones = drafts
        )
    }

    private fun updateMilestone(id: String, transform: (MilestoneInput) -> MilestoneInput) {
        mutableState.update { state ->
            state.copy(milestones = state.milestones.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

    private fun Assignment.toCreationState(): AdminCreationState {
        val zone = ZoneId.of("America/Santo_Domingo")
        val inputs = milestones.sortedBy { it.order }.map { milestone ->
            val dateTime = milestone.scheduledAt?.let { value ->
                runCatching { java.time.OffsetDateTime.parse(value).atZoneSameInstant(zone) }.getOrNull()
            }
            MilestoneInput(
                id = milestone.id,
                title = milestone.title,
                date = dateTime?.toLocalDate()?.toString() ?: LocalDate.now().plusDays(1).toString(),
                time = dateTime?.toLocalTime()?.withSecond(0)?.withNano(0)?.toString() ?: "09:00",
                collaboratorIds = milestone.collaborators.map { it.userId }.toSet(),
                status = MilestoneStatus.entries.firstOrNull { it.value == milestone.status }
            )
        }.ifEmpty { listOf(MilestoneInput()) }
        return AdminCreationState(
            editing = true,
            page = CreationPage.Form,
            selectedType = if (type == AssignmentFlowType.Field.value) CreationType.Field else CreationType.Administrative,
            supervisorIds = supervisors.map { it.userId }.toSet(),
            title = title,
            location = location.orEmpty(),
            priority = AssignmentPriority.entries.firstOrNull { it.value == priority } ?: AssignmentPriority.Low,
            instructions = instructions.orEmpty(),
            milestones = inputs
        )
    }
}
