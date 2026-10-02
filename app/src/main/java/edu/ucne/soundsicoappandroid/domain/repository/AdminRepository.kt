package edu.ucne.soundsicoappandroid.domain.repository

import edu.ucne.soundsicoappandroid.domain.model.*

interface AdminRepository {
    suspend fun getDashboardMetrics(): DashboardMetrics
    suspend fun getAssignments(offset: Int, limit: Int, filter: AdminAssignmentFilter, search: String): AdminAssignmentPage
    suspend fun getEmployees(): List<Employee>
    suspend fun createAssignment(draft: AssignmentDraft): Assignment
    suspend fun updateAssignment(id: String, draft: AssignmentDraft): Assignment
    suspend fun deleteAssignment(id: String)
    suspend fun getAccounts(): List<ManagedAccount>
    suspend fun reviewAccount(userId: String, state: AccountAccessState)
    suspend fun getEmployeePerformance(month: String, employeeId: String? = null): List<EmployeePerformance>
    suspend fun getEmployeeAvailability(window: AssignmentBookingWindow, excludingAssignmentId: String? = null): List<EmployeeAvailability>
}
