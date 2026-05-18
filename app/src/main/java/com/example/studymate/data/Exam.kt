package com.example.studymate.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val examName: String,
    val destination: String,
    val examTime: String,
    val examDate: String
)
