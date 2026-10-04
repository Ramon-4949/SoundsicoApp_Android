package edu.ucne.soundsicoappandroid

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import edu.ucne.soundsicoappandroid.core.designsystem.SoundiscoTheme
import edu.ucne.soundsicoappandroid.domain.model.*
import edu.ucne.soundsicoappandroid.presentation.home.*
import edu.ucne.soundsicoappandroid.presentation.login.*
import edu.ucne.soundsicoappandroid.presentation.signup.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AuthenticationScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loginExposesFieldsAndDispatchesSubmit() {
        val intents = mutableListOf<LoginIntent>()
        compose.setContent {
            SoundiscoTheme {
                LoginScreen(
                    state = LoginState(),
                    onIntent = intents::add,
                    onSignUp = {},
                    onRecoverPassword = {}
                )
            }
        }
        compose.onNodeWithContentDescription("Correo electrónico").performTextInput("ana@empresa.com")
        compose.onNodeWithContentDescription("Contraseña").performTextInput("Segura!928")
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        assertTrue(intents.any { it is LoginIntent.EmailChanged })
        assertTrue(intents.any { it is LoginIntent.PasswordChanged })
        assertTrue(intents.contains(LoginIntent.Submit))
    }

    @Test fun registrationCanReachConsentAndSubmit() {
        val intents = mutableListOf<SignUpIntent>()
        compose.setContent { SoundiscoTheme { SignUpScreen(SignUpState(), intents::add, {}) } }
        compose.onNodeWithContentDescription("Nombre de usuario").assertExists()
        compose.onNodeWithContentDescription("Número de teléfono").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Acepto los términos de servicio y la política de privacidad.").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Crear cuenta").onLast().performScrollTo().performClick()
        assertTrue(intents.contains(SignUpIntent.Submit))
    }

    @Test fun pendingAccountHidesProtectedNavigation() {
        val user = AuthUser("u", "ana@empresa.com", "Ana")
        val profile = EmployeeProfile("u", "Ana", "ana", "", "Técnico", "empleado", AccountAccess.Pending)
        compose.setContent { SoundiscoTheme { HomeScreen(HomeState(user, profile, loading = false), {}) } }
        compose.onNodeWithText("Cuenta en revisión").assertIsDisplayed()
        compose.onNodeWithText("Consultar estado").assertIsDisplayed()
        compose.onNodeWithText("Calendario").assertDoesNotExist()
    }

    @Test fun approvedAccountCanSelectProfile() {
        val intents = mutableListOf<HomeIntent>()
        val user = AuthUser("u", "ana@empresa.com", "Ana")
        val profile = EmployeeProfile("u", "Ana", "ana", "", "Técnico", "empleado", AccountAccess.Approved)
        compose.setContent { SoundiscoTheme { HomeScreen(HomeState(user, profile, loading = false), intents::add) } }
        compose.onNodeWithText("Perfil").performClick()
        assertTrue(intents.contains(HomeIntent.SelectTab(HomeTab.Profile)))
    }
}
