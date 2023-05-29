package com.eneskoc.todolist.roomdb

import androidx.room.*
import com.eneskoc.todolist.model.Tasks
import java.util.*

@Dao
interface TasksDao {

    @Query("SELECT * FROM Tasks")
    suspend fun getAll(): List<Tasks>

    @Query("SELECT * FROM Tasks WHERE taskDate = :taskDate")
    suspend fun getSelectedDateTask(taskDate: String) : List<Tasks>

    @Insert
    suspend fun insertTask(task: Tasks)

    @Delete
    suspend fun deleteTask(task: Tasks)

    @Query("UPDATE Tasks SET taskName = :taskName,taskDescription = :taskDescription, taskDate = :taskDate, taskTime = :taskTime, taskPriority = :taskPriority, isCompleted = :completed, taskReminder = :taskReminder WHERE taskId = :id")
    fun updateTask(taskName: String, taskDescription:String, taskDate: String, taskTime: String, taskPriority: Int, completed: Boolean, taskReminder:Boolean ,id: Int)

    @Query("UPDATE Tasks SET isCompleted = :completed WHERE taskId = :id")
    fun updateTaskCompletionStatus(id: Int, completed: Boolean)
}