package edu.ucne.soundsicoappandroid.domain.validation

import edu.ucne.soundsicoappandroid.domain.model.Registration

object AuthValidation {
    val positions = listOf(
        "Técnico de sonido", "Técnico audiovisuales", "Técnico de iluminación",
        "Encargado de estructura", "Encargado de almacén", "Supervisor",
        "Administrativo", "Contabilidad", "Recursos Humanos", "Marketing Digital",
        "Chofer/Técnico de sonido", "Chofer/Técnico de audiovisuales",
        "Chofer/Técnico de iluminación", "Chofer/Encargado de estructura",
        "Chofer/Encargado de almacén"
    )

    fun email(value: String): String? {
        val clean = value.trim()
        return when {
            clean.isEmpty() -> "El correo electrónico es obligatorio."
            clean.length > 254 || clean.contains("..") ||
                !Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,63}$").matches(clean) ->
                "Introduce un correo electrónico válido."
            else -> null
        }
    }

    fun password(value: String, personalValues: List<String> = emptyList()): String? = when {
        value.length !in 8..72 -> "Debe tener entre 8 y 72 caracteres."
        value.any(Char::isWhitespace) -> "No puede contener espacios."
        !value.any(Char::isLowerCase) -> "Añade al menos una letra minúscula."
        !value.any(Char::isUpperCase) -> "Añade al menos una letra mayúscula."
        !value.any(Char::isDigit) -> "Añade al menos un número."
        value.none { !it.isLetterOrDigit() && !it.isWhitespace() && !it.isISOControl() } ->
            "Añade al menos un símbolo."
        personalValues.map(String::trim).any { it.length >= 3 && value.contains(it, ignoreCase = true) } ->
            "No incluyas tu nombre, usuario o correo en la contraseña."
        else -> null
    }

    fun registration(value: Registration): Map<String, String> = buildMap {
        val username = value.username.trim()
        val name = value.fullName.trim()
        val phone = value.phone.trim()
        if (username.length !in 3..30 || !Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$").matches(username))
            put("username", "Usa entre 3 y 30 caracteres: letras, números, punto o guion.")
        if (name.length !in 2..80 || name.any { !it.isLetter() && it !in " '-’" })
            put("fullName", "Introduce un nombre de 2 a 80 caracteres, sin números.")
        email(value.email)?.let { put("email", it) }
        if (phone.count(Char::isDigit) !in 10..15 || phone.any { !it.isDigit() && it !in "+() -" })
            put("phone", "Introduce un teléfono válido de 10 a 15 dígitos.")
        if (value.position !in positions) put("position", "Selecciona un cargo válido.")
        password(value.password, listOf(username, name, value.email.substringBefore("@")))?.let { put("password", it) }
        if (value.confirmation.isEmpty() || value.confirmation != value.password)
            put("confirmation", "Las contraseñas deben coincidir.")
        if (!value.acceptedTerms) put("terms", "Debes aceptar los términos y la política de privacidad.")
    }
}
