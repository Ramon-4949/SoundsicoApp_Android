package edu.ucne.soundsicoappandroid.domain.model

data class Assignment(
    val id: String,
    val title: String,
    val type: String,
    val priority: String,
    val status: String,
    val location: String?,
    val deadline: String?,
    val instructions: String?,
    val scheduledDates: List<String> = emptyList()
)

data class Bulletin(val id: String, val subject: String, val message: String)

data class HomeContent(val assignments: List<Assignment>, val bulletins: List<Bulletin>)
