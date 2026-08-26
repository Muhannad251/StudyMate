package com.example.studymate.data

data class GruppenAufgabe(
    val id: String = "",
    val titel: String = "",
    val modulName: String = "",
    val erledigt: Boolean = false,
    val erstelltVon: String = "",
    val erledigtVon: String = "",
    val proofVorhanden: Boolean = false,
    val erstelltAm: Long = System.currentTimeMillis(),
    val erledigtAm: Long = 0L
)