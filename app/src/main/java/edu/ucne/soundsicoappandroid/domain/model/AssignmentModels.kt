package edu.ucne.soundsicoappandroid.domain.model

enum class AssignmentFlowType(val value: String) {
    Field("campo"),
    Administrative("administrativa")
}

enum class AssignmentPriority(val value: String) {
    Low("baja"),
    Medium("media"),
    High("alta")
}

enum class AssignmentStatus(val value: String) {
    Pending("pendiente"),
    InProgress("en_curso"),
    Overdue("vencida"),
    Completed("completada")
}

enum class EmployeeRole(val value: String) {
    Employee("empleado"),
    Technician("tecnico"),
    Administrator("admin")
}

data class Employee(
    val id: String,
    val name: String,
    val role: String,
    val position: String? = null
)

data class AssignmentSupervisor(val userId: String)

data class AssignmentDraft(
    val id: String? = null,
    val supervisorIds: List<String> = emptyList(),
    val flowType: AssignmentFlowType,
    val title: String,
    val location: String? = null,
    val priority: AssignmentPriority,
    val instructions: String? = null,
    val status: AssignmentStatus = AssignmentStatus.Pending,
    val employeeIds: List<String>,
    val createdAt: String,
    val deadline: String? = null,
    val milestones: List<MilestoneDraft>
)

enum class AdminAssignmentFilter(val value: String) {
    All("todas"),
    Pending("pendientes"),
    Overdue("vencidas"),
    Completed("completadas")
}

data class AssignmentDetails(
    val assignment: Assignment,
    val checkIns: List<MilestoneCheckIn>,
    val notes: List<AssignmentNote>,
    val collaborators: List<AssignmentCollaborator>
)
