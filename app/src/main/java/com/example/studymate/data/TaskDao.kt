package com.example.studymate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TaskDao {

    @Insert
    suspend fun insertTask(task : Task)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Query("UPDATE tasks SET isDone = :isDone WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isDone: Boolean)

}