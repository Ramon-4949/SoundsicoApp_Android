package edu.ucne.soundsicoappandroid.data.remote

import edu.ucne.soundsicoappandroid.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DashboardMetricsDto(
    val total: Int,
    @SerialName("unidades_asignadas") val assigned: Int,
    @SerialName("unidades_activas") val active: Int,
    @SerialName("tasa_entrega") val deliveryRate: Double,
    @SerialName("creadas_ultimos_7_dias") val createdLastSevenDays: List<Int>
) {
    fun toDomain() = DashboardMetrics(total, assigned, active, deliveryRate, createdLastSevenDays)
}

@Serializable
data class AssignmentPageDto(
    val items: List<AssignmentDto>,
    @SerialName("has_more") val hasMore: Boolean,
    @SerialName("next_offset") val nextOffset: Int? = null
) {
    fun toDomain() = AssignmentPage(items.map { it.toDomain("", true) }, nextOffset.takeIf { hasMore })
}

@Serializable
data class ManagedAccountDto(
    val id: String,
    val nombre: String? = null,
    val email: String? = null,
    val cargo: String? = null,
    val estado: String
) {
    fun toDomain() = PendingAccount(id, nombre?.takeIf { it.isNotBlank() } ?: "Sin nombre", email.orEmpty(), cargo.orEmpty())
}
