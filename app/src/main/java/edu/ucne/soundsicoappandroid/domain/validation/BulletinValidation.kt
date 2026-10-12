package edu.ucne.soundsicoappandroid.domain.validation

object BulletinValidation {
    const val SUBJECT_LIMIT = 140
    const val MESSAGE_LIMIT = 4000

    fun subjectError(value: String): String? = when {
        value.trim().length < 3 -> "El asunto debe tener al menos 3 caracteres."
        value.length > SUBJECT_LIMIT -> "El asunto no puede superar $SUBJECT_LIMIT caracteres."
        else -> null
    }

    fun messageError(value: String): String? = when {
        value.trim().length < 10 -> "El mensaje debe tener al menos 10 caracteres."
        value.length > MESSAGE_LIMIT -> "El mensaje no puede superar $MESSAGE_LIMIT caracteres."
        else -> null
    }
}
