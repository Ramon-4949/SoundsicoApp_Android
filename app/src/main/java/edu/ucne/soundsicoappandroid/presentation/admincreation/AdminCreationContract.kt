package edu.ucne.soundsicoappandroid.presentation.admincreation

import edu.ucne.soundsicoappandroid.domain.model.AssignmentPriority
import edu.ucne.soundsicoappandroid.domain.model.Employee
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import edu.ucne.soundsicoappandroid.domain.model.MilestoneStatus

enum class CreationType {
    Field,
    Administrative,
    Message
}

enum class CreationPage {
    Type,
    Form,
    Responsibles
}

enum class ResponsibleTarget {
    Milestone,
    Supervisors
}

data class MilestoneInput(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val date: String = LocalDate.now().plusDays(1).toString(),
    val time: String = LocalTime.of(9, 0).toString(),
    val collaboratorIds: Set<String> = emptySet(),
    val status: MilestoneStatus? = null
)

data class AdminCreationState(
    val editing: Boolean = false,
    val page: CreationPage = CreationPage.Type,
    val selectedType: CreationType? = null,
    val loadingEmployees: Boolean = false,
    val employees: List<Employee> = emptyList(),
    val supervisorIds: Set<String> = emptySet(),
    val title: String = "",
    val location: String = "",
    val priority: AssignmentPriority = AssignmentPriority.Low,
    val instructions: String = "",
    val milestones: List<MilestoneInput> = listOf(MilestoneInput()),
    val subject: String = "",
    val message: String = "",
    val responsibleTarget: ResponsibleTarget? = null,
    val responsibleMilestoneId: String? = null,
    val responsibleSearch: String = "",
    val responsibleCategory: String = "Todos",
    val saving: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false
)

sealed interface AdminCreationIntent {
    data class SelectType(val value: CreationType) : AdminCreationIntent
    data object Continue : AdminCreationIntent
    data object Back : AdminCreationIntent
    data class ChangeTitle(val value: String) : AdminCreationIntent
    data class ChangeLocation(val value: String) : AdminCreationIntent
    data class ChangePriority(val value: AssignmentPriority) : AdminCreationIntent
    data class ChangeInstructions(val value: String) : AdminCreationIntent
    data class ToggleMilestoneEmployee(val milestoneId: String, val employeeId: String) : AdminCreationIntent
    data class ToggleSupervisor(val id: String) : AdminCreationIntent
    data class OpenMilestoneResponsibles(val milestoneId: String) : AdminCreationIntent
    data object OpenSupervisorResponsibles : AdminCreationIntent
    data class ChangeResponsibleSearch(val value: String) : AdminCreationIntent
    data class ChangeResponsibleCategory(val value: String) : AdminCreationIntent
    data object ConfirmResponsibles : AdminCreationIntent
    data class ChangeMilestoneTitle(val id: String, val value: String) : AdminCreationIntent
    data class ChangeMilestoneDate(val id: String, val value: String) : AdminCreationIntent
    data class ChangeMilestoneTime(val id: String, val value: String) : AdminCreationIntent
    data object AddMilestone : AdminCreationIntent
    data class RemoveMilestone(val id: String) : AdminCreationIntent
    data class ChangeSubject(val value: String) : AdminCreationIntent
    data class ChangeMessage(val value: String) : AdminCreationIntent
    data object Submit : AdminCreationIntent
    data object DismissError : AdminCreationIntent
}
