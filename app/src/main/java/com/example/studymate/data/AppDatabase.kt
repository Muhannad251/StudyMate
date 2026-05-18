package com.example.studymate.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Exam::class, Task::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun examDao() : ExamDao
    abstract fun taskDao():TaskDao
}