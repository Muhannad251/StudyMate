package com.example.studymate.data

data class Gruppe(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val erstelltVon: String = "",
    val erstelltAm: Long = System.currentTimeMillis(),
    val members: List<String> = emptyList()
)