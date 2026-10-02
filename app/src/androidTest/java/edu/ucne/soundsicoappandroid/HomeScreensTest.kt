package edu.ucne.soundsicoappandroid

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import edu.ucne.soundsicoappandroid.core.designsystem.SoundiscoTheme
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.presentation.home.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class HomeScreensTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.now().atTime(6, 30).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
    private val assignments = listOf(
        Assignment("a", "Montaje de Sonido Principal", "campo", "alta", "pendiente", "Hotel las Caobas 7PXM+M5G", date, null,
            listOf(date), "Llegada al almacén", date),
        Assignment("b", "Cotización de boda de Santiago 2", "administrativa", "alta", "completada", null, date, null,
            listOf(date), "Hasta las 12pm", date)
    )

    private fun state(admin: Boolean) = HomeState(
        AuthUser("u", "ana@empresa.com", if (admin) "Yankee" else "JoseCarlos01"),
        EmployeeProfile("u", "Ana", if (admin) "Yankee" else "JoseCarlos01", "", "Técnico",
            if (admin) "admin" else "empleado", AccountAccess.Approved),
        loading = false,
        content = HomeContent(assignments, emptyList(), if (admin) AdminDashboard(
            DashboardMetrics(17, 5, 2, 98.4, listOf(1, 2, 0, 4, 3, 2, 5)),
            listOf(PendingAccount("p", "Ana Pérez", "ana@empresa.com", "Técnico"))
        ) else null)
    )

    @Test fun adminShowsMetricsAndWorkingApprovalShortcut() {
        val intents = mutableListOf<HomeIntent>()
        compose.setContent { SoundiscoTheme { HomeScreen(state(true), intents::add) } }
        compose.onNodeWithText("Asignadas").assertIsDisplayed()
        compose.onNodeWithText("En progreso").assertIsDisplayed()
        capture("home-admin.png")
        compose.onNodeWithText("Revisar aprobaciones (1)").performClick()
        assertTrue(intents.contains(HomeIntent.OpenApprovals))
    }

    @Test fun employeeShowsDailyCardsWithoutAdminControls() {
        val intents = mutableListOf<HomeIntent>()
        compose.setContent { SoundiscoTheme { HomeScreen(state(false), intents::add) } }
        compose.onNodeWithText("Montaje de Sonido Principal").assertIsDisplayed()
        compose.onNodeWithText("Asignadas").assertDoesNotExist()
        compose.onNodeWithText("Revisar aprobaciones (1)").assertDoesNotExist()
        capture("home-employee.png")
        compose.onNodeWithText("Ver todas (2)").performClick()
        assertTrue(intents.contains(HomeIntent.ShowAll))
    }

    @Test fun approvalRequiresExplicitConfirmation() {
        val intents = mutableListOf<HomeIntent>()
        compose.setContent { SoundiscoTheme { HomeScreen(state(true).copy(approvalsOpen = true), intents::add) } }
        compose.onNodeWithText("Ana Pérez").assertIsDisplayed()
        compose.onNodeWithText("Aprobar").performClick()
        assertTrue(intents.any { it is HomeIntent.RequestReview })
        assertFalse(intents.contains(HomeIntent.ConfirmReview))
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), name)
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
