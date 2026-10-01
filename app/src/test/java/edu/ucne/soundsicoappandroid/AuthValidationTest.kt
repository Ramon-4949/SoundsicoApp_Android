package edu.ucne.soundsicoappandroid

import edu.ucne.soundsicoappandroid.domain.model.Registration
import edu.ucne.soundsicoappandroid.domain.validation.AuthValidation
import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {
    private val valid = Registration("ana.perez", "Ana Pérez", "ana@empresa.com",
        "+1 (809) 555-1234", "Técnico de sonido", "Segura!928", "Segura!928", true)

    @Test fun acceptsValidRegistration() {
        assertTrue(AuthValidation.registration(valid).isEmpty())
    }

    @Test fun rejectsPersonalPasswordAndMissingConsent() {
        val errors = AuthValidation.registration(valid.copy(password = "ana.perez!928A", confirmation = "", acceptedTerms = false))
        assertTrue(errors.keys.containsAll(listOf("password", "confirmation", "terms")))
    }

    @Test fun rejectsInvalidIdentityAndRole() {
        val errors = AuthValidation.registration(valid.copy(username = "@ana", fullName = "Ana2",
            email = "ana..perez@empresa.com", phone = "123", position = "admin"))
        assertTrue(errors.keys.containsAll(listOf("username", "fullName", "email", "phone", "position")))
    }

    @Test fun rejectsWeakPasswords() {
        listOf("12345678", "SoloLetras", "SinNumero!", "con numero1!", "ConEspacio 9!", "Corta1!").forEach {
            assertNotNull(AuthValidation.password(it))
        }
    }
}
