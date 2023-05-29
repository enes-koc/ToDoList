package com.eneskoc.todolist.view


import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import com.eneskoc.todolist.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.eneskoc.todolist.adapter.TasksAdapter
import com.eneskoc.todolist.databinding.FragmentCalendarBinding
import com.eneskoc.todolist.model.Tasks
import com.eneskoc.todolist.roomdb.TasksDao
import com.eneskoc.todolist.roomdb.TasksDatabase
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.time.Duration.Companion.days


class CalendarPage : Fragment(), TasksAdapter.OnItemClickListener {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var onItemClickListener: TasksAdapter.OnItemClickListener
    private lateinit var db: TasksDatabase
    private lateinit var tasksDao: TasksDao
    private var job: Job? = null
    private lateinit var allTaskList: List<Tasks>
    private lateinit var taskList: List<Tasks>
    private lateinit var tasksAdapter: TasksAdapter

    private lateinit var currentMonth: YearMonth
    private lateinit var imageEvent1: ImageView
    private lateinit var imageEvent2: ImageView
    private lateinit var imageEvent3: ImageView
    private lateinit var textViewDay: TextView
    private lateinit var calenderDayLayout: LinearLayout
    private lateinit var day: CalendarDay
    var selectedDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = context?.let {
            Room.databaseBuilder(
                it.applicationContext,
                TasksDatabase::class.java,
                "Tasks"
            ).build()
        }!!
        tasksDao = db.tasksDao()
        getData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        job = CoroutineScope(Dispatchers.IO).launch {
            allTaskList = tasksDao.getAll()
        }

        binding.calendarRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
        onItemClickListener = this

        //getSelectedDateData(LocalDate.parse(test).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) // Seçili datanın tasklarını getir
        //Ay değiştiğinde
        binding.calendarView.monthScrollListener = { month ->
            binding.monthHeaderText.text = "${month.yearMonth.month} ${month.yearMonth.year}"
        }


        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {

            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.calendarView = binding.calendarView
                container.day = data
                textViewDay = container.textViewDay
                calenderDayLayout = container.calenderDayLayout
                imageEvent1 = container.imageEvent1
                imageEvent2 = container.imageEvent2
                imageEvent3 = container.imageEvent3
                day = data
                textViewDay.text = data.date.dayOfMonth.toString()
                selectDate(container)
            }
        }
        createWeekHeader(view)
        createCalendar()
    }

    private fun createCalendar() {
        currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(2)  // Adjust as needed
        val endMonth = currentMonth.plusMonths(30)  // Adjust as needed
        val firstDayOfWeek = firstDayOfWeekFromLocale() // Available from the library
        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)
    }

    private fun createWeekHeader(view: View) {
        val daysOfWeek = daysOfWeek()
        val titlesContainer = view.findViewById<ViewGroup>(R.id.titlesContainer)
        titlesContainer.children.map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }
    }

    private fun selectDate(container: DayViewContainer) {

        if (day.position == DayPosition.MonthDate) {
            // Show the month dates. Remember that views are reused!
            markTasksOnCalender(day.date)
            textViewDay.visibility = View.VISIBLE
            if (day.date == container.selectedDate) {

                // If this is the selected date, show a round background and change the text color.
                selectedDate = container.selectedDate
                textViewDay.setTextColor(Color.WHITE)
                calenderDayLayout.setBackgroundResource(R.drawable.baseline_crop_square_24)

                getSelectedDateData(LocalDate.parse(container.selectedDate.toString()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) // Seçili datanın tasklarını getir
                binding.calendarSelectedDateHeader.text= day.date.month.getDisplayName(TextStyle.SHORT,getResources().getConfiguration().getLocales().get(0)) +" "+day.date.dayOfMonth.toString()
                container.selectedDate = null
            } else {
                // If this is NOT the selected date, remove the background and reset the text color.
                textViewDay.setTextColor(Color.BLACK)
                calenderDayLayout.background=null
            }
        } else {
            // Hide in and out dates
            textViewDay.visibility = View.INVISIBLE
        }
    }

    private fun markTasksOnCalender(date:LocalDate){
        val taskDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        val taskDate = localDate.format(taskDateFormatter)

        //println(date.toString()+" tarihinden taskList içerisinde "+  taskList.count { it.taskDate == taskDate }+" adet var.")
        when (allTaskList.count { it.taskDate == taskDate }) {
            0 ->{imageEvent1.visibility = View.GONE
                imageEvent2.visibility = View.GONE
                imageEvent3.visibility = View.GONE
            }
            1 -> {imageEvent1.visibility = View.VISIBLE
                imageEvent2.visibility = View.GONE
                imageEvent3.visibility = View.GONE
            }
            2 -> {
                imageEvent1.visibility = View.VISIBLE
                imageEvent2.visibility = View.VISIBLE
                imageEvent3.visibility = View.GONE
            }
            else -> {
                imageEvent1.visibility = View.VISIBLE
                imageEvent2.visibility = View.VISIBLE
                imageEvent3.visibility = View.VISIBLE
            }
        }
    }


    private fun getData() {
        job = CoroutineScope(Dispatchers.IO).launch {
            taskList = tasksDao.getAll()
        }
    }

    private fun getSelectedDateData(taskDate: String) {
        job = CoroutineScope(Dispatchers.IO).launch {
            taskList = tasksDao.getSelectedDateTask(taskDate)
                .sortedWith(compareBy<Tasks> { it.isCompleted }.thenBy {
                    LocalDate.parse(
                        it.taskDate,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    )
                })
            tasksAdapter = TasksAdapter(taskList, onItemClickListener)
            withContext(Dispatchers.Main) {
                binding.calendarRecyclerView.adapter = tasksAdapter
                tasksAdapter.notifyDataSetChanged()
                binding.calendarRecyclerView.scrollToPosition(taskList.size - 1)
            }
        }
    }

    private fun updateTaskIsComplateStatus(tasks: Tasks) {
        job = CoroutineScope(Dispatchers.IO).launch {
            if (tasks.isCompleted) {
                tasksDao.updateTaskCompletionStatus(tasks.taskId, false)
            } else {
                tasksDao.updateTaskCompletionStatus(tasks.taskId, true)
            }
            withContext(Dispatchers.Main) {
                getSelectedDateData(tasks.taskDate)
            }
        }
    }

    override fun onClick(task: Tasks) {
        updateTaskIsComplateStatus(task)
    }
}