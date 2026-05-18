package com.example.studymate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExamDao{

    @Insert
    suspend fun insertExam(exam:Exam)

    @Query("SELECT * FROM exams")
    suspend fun getAllExams(): List<Exam>

}