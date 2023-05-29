package com.eneskoc.todolist.view


import android.opengl.Visibility
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.room.Room
import com.eneskoc.todolist.R
import com.eneskoc.todolist.model.Tasks
import com.eneskoc.todolist.roomdb.TasksDao
import com.eneskoc.todolist.roomdb.TasksDatabase
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class DayViewContainer(view: View) : ViewContainer(view) {

    var calendarView = view.findViewById<CalendarView>(R.id.calendarView)
    var calenderDayLayout = view.findViewById<LinearLayout>(R.id.calenderDayLayout)
    val textViewDay = view.findViewById<TextView>(R.id.calendarDayText)
    val imageEvent1 = view.findViewById<ImageView>(R.id.imageEvent1)
    val imageEvent2 = view.findViewById<ImageView>(R.id.imageEvent2)
    val imageEvent3 = view.findViewById<ImageView>(R.id.imageEvent3)

    lateinit var day: CalendarDay
    var selectedDate: LocalDate? = null


    init {

        view.setOnClickListener {
            if (day.position == DayPosition.MonthDate) {
                selectedDate = day.date
                calendarView.notifyCalendarChanged()
            }
        }
    }
}