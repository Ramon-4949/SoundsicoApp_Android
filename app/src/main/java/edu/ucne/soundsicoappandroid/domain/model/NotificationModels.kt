package edu.ucne.soundsicoappandroid.domain.model

enum class NotificationType(val value: String) {
    Assignment("asignacion"),
    StatusChange("cambio_estado"),
    Bulletin("comunicado"),
    System("sistema")
}

data class NotificationHistory(
    val id: String,
    val type: NotificationType,
    val message: String,
    val createdAt: String
)

data class EmployeeNotification(
    val id: String,
    val title: String?,
    val message: String?,
    val read: Boolean,
    val createdAt: String?,
    val type: String?,
    val destinationId: String?,
    val destinationType: String?,
    val status: String?
)
