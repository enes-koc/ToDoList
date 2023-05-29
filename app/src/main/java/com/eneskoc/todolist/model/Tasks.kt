package com.eneskoc.todolist.model

import android.app.ActivityManager.TaskDescription
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
class Tasks(

    @ColumnInfo(name="taskName")
    var taskName: String,

    @ColumnInfo(name="taskDescription")
    var taskDescription : String,

    @ColumnInfo(name="taskDate")
    var taskDate: String,

    @ColumnInfo(name="taskTime")
    var taskTime: String,

    @ColumnInfo(name="taskPriority")
    var taskPriority: Int,

    @ColumnInfo(name="isCompleted")
    var isCompleted:Boolean,

    @ColumnInfo(name="taskReminder")
    var taskReminder:Boolean,

    ) {
    @PrimaryKey(autoGenerate = true)
    var taskId=0
}