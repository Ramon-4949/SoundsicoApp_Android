package edu.ucne.soundsicoappandroid.core.presentation

import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException

fun Throwable.userMessage(): String = when (this) {
    is AuthRestException -> when {
        message.orEmpty().contains("invalid_credentials", true) ||
            message.orEmpty().contains("Invalid login credentials", true) -> "El correo o la contraseña no son correctos."
        message.orEmpty().contains("already", true) -> "Ya existe una cuenta con este correo."
        message.orEmpty().contains("rate", true) -> "Demasiados intentos. Espera unos minutos e inténtalo nuevamente."
        message.orEmpty().contains("email_not_confirmed", true) -> "Confirma tu correo antes de iniciar sesión."
        else -> "No se pudo completar la autenticación. Revisa tus datos e inténtalo nuevamente."
    }
    is IOException -> "No se pudo conectar. Comprueba tu conexión e inténtalo nuevamente."
    is RestException -> "No se pudo consultar el servidor. Inténtalo nuevamente."
    is IllegalArgumentException, is IllegalStateException -> message ?: "No se pudo completar la operación."
    else -> "Ocurrió un error. Inténtalo nuevamente."
}
