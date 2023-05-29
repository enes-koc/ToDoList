package com.eneskoc.todolist.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.eneskoc.todolist.model.Tasks

@Database(entities = [Tasks::class], version = 1)
abstract class TasksDatabase : RoomDatabase() {
    abstract fun tasksDao(): TasksDao
}
