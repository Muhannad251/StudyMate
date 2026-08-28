package com.example.studymate.data

data class GruppenNachricht(
    val id: String = "",
    val text: String = "",
    val absenderId: String = "",
    val absenderName: String = "",
    val erstelltAm: Long = 0L
)