package com.eneskoc.todolist.view

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CalendarView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.widget.addTextChangedListener
import androidx.room.Room
import androidx.room.util.query
import com.eneskoc.todolist.R
import com.eneskoc.todolist.adapter.TasksAdapter
import com.eneskoc.todolist.databinding.FragmentTaskEditPageBinding
import com.eneskoc.todolist.model.Tasks
import com.eneskoc.todolist.roomdb.TasksDao
import com.eneskoc.todolist.roomdb.TasksDatabase
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


class TaskEditPage(var task: Tasks) : Fragment() {

    private var _binding: FragmentTaskEditPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: TasksDatabase
    private lateinit var tasksDao: TasksDao
    private var job: Job? = null

    private var taskName = task.taskName
    private var taskDescription = task.taskDescription
    private var taskDate = task.taskDate
    private var taskTime = task.taskTime
    private var taskPriority = task.taskPriority
    private var isComplated = task.isCompleted
    private var taskReminder=task.taskReminder


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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskEditPageBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.taskEditPageTaskIsComplated.isChecked = task.isCompleted
        binding.taskEditPageTaskDate.text = formatDate(task.taskDate) + " " + task.taskTime
        binding.taskEditPageTaskName.setText(task.taskName)
        binding.taskEditPageDescription.setText(task.taskDescription)
        setTaskPriorityImage(task.taskPriority)
        if(task.taskReminder) binding.recyclerViewReminderImageView.visibility=View.VISIBLE else binding.recyclerViewReminderImageView.visibility=View.GONE

        binding.taskEditPageTaskIsComplated.setOnCheckedChangeListener { buttonView, isChecked ->
            isComplated = isChecked
            updateTask()
        }

        binding.taskEditPageTaskName.setOnFocusChangeListener { v, hasFocus ->
            taskName = binding.taskEditPageTaskName.text.toString()
            updateTask()
        }

        binding.taskEditPageDescription.setOnFocusChangeListener { v, hasFocus ->
            taskDescription = binding.taskEditPageDescription.text.toString()
            updateTask()
        }

        binding.taskEditPageTaskDate.setOnClickListener {
            datePickerPopupMenu()
            updateTask()
        }

        binding.taskEditPagePriorityImageView.setOnClickListener {
            priorityPopupMenu()
            updateTask()
        }
    }

    fun setTaskPriorityImage(taskPriority: Int) {
        when (taskPriority) {
            0 -> {
                binding.taskEditPagePriorityImageView.setImageResource(R.drawable.prioirty_no_flag_24)
            }
            1 -> {
                binding.taskEditPagePriorityImageView.setImageResource(R.drawable.priority_low_flag_24)
            }
            2 -> {
                binding.taskEditPagePriorityImageView.setImageResource(R.drawable.priority_medium_flag_24)
            }
            3 -> {
                binding.taskEditPagePriorityImageView.setImageResource(R.drawable.priority_high_flag_24)
            }
        }
    }

    fun formatDate(date: String): String {

        val tempDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val formattedDate = tempDate.format(DateTimeFormatter.ofPattern("MMM d"))

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val inputDate = LocalDate.parse(date, formatter)
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(today, inputDate)

        if (days > 0) {
            return "$formattedDate, $days" + "d left"
        } else if (days < 0) {
            return "$formattedDate, $days" + "d ago"
        } else {
            return "$formattedDate, today"
        }
    }

    private fun priorityPopupMenu() {
        val popupMenu = PopupMenu(context, binding.taskEditPagePriorityImageView)
        popupMenu.inflate(R.menu.priority_selection_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.noPriority -> {
                    taskPriority = 0
                    binding.taskEditPagePriorityImageView.setImageResource(R.drawable.prioirty_no_flag_24)
                    true
                }
                R.id.lowPriority -> {
                    taskPriority = 1
                    binding.taskEditPagePriorityImageView.setImageResource(R.drawable.priority_low_flag_24)
                    true
                }
                R.id.mediumPriority -> {
                    taskPriority = 2
                    binding.taskEditPagePriorityImageView.setImageResource(R.drawable.priority_medium_flag_24)
                    true
                }
                R.id.highPriority -> {
                    taskPriority = 3
                    binding.taskEditPagePriorityImageView.setImageResource(R.drawable.priority_high_flag_24)
                    true
                }
                else -> true
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true)
        }

        popupMenu.setOnDismissListener {
            updateTask()
        }

        popupMenu.show()
    }

    private fun datePickerPopupMenu() {

        var currentDay = ""
        var currentYear = ""
        var currentMonth = ""


        val inflater = requireActivity().layoutInflater;
        val builder = AlertDialog.Builder(context)
        val dialogView = inflater.inflate(R.layout.task_date_selection_menu, null)

        val calendarView =
            dialogView.findViewById<CalendarView>(R.id.task_date_selection_menu_calender)

        val dialog = builder.setView(dialogView)
            // Add action buttons
            .setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, id ->
                    if (currentDay.isNotEmpty() && currentMonth.isNotEmpty() && currentYear.isNotEmpty()) {
                        taskDate = currentDay + "/" + currentMonth + "/" + currentYear
                    }
                    binding.taskEditPageTaskDate.text = formatDate(taskDate) + " " + taskTime
                    updateTask()

                    if (taskReminder) {
                        scheduleNotification(task.taskId)
                    }else{
                        cancelNotification(task.taskId)
                    }
                    (dialog as AlertDialog).dismiss()
                })
            .setNegativeButton("CANCEL",
                DialogInterface.OnClickListener { dialog, id ->
                    (dialog as AlertDialog).dismiss()
                }).show()

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.mainColor400, typedValue, true)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(typedValue.data)
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(typedValue.data)

        calendarView.minDate = Date().time
        calendarView
            .setOnDateChangeListener { view, year, month, dayOfMonth ->
                currentDay = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()
                currentYear = java.lang.String.valueOf(year)
                val tempMonth = month + 1
                currentMonth = if (tempMonth < 10) "0$tempMonth" else tempMonth.toString()
            }

        if (taskReminder) { dialogView.findViewById<Button>(R.id.task_date_selection_menu_setReminderButton).text = "CANCEL REMINDER"
            binding.recyclerViewReminderImageView.visibility=View.VISIBLE}
        else { dialogView.findViewById<Button>(R.id.task_date_selection_menu_setReminderButton).text = "SET REMINDER"
            binding.recyclerViewReminderImageView.visibility=View.GONE}

        dialogView.findViewById<Button>(R.id.task_date_selection_menu_setTimeButton)
            .setOnClickListener {
                timePickerPopupMenu(dialogView)
            }

        dialogView.findViewById<Button>(R.id.task_date_selection_menu_setReminderButton)
            .setOnClickListener {
                setReminder(dialogView)
                if (taskReminder) { binding.recyclerViewReminderImageView.visibility=View.VISIBLE}
                else { binding.recyclerViewReminderImageView.visibility=View.GONE} }


    }

    private fun setReminder(dialogView: View){

        if (taskTime=="") {
            Toast.makeText(requireContext(),"You need to set a time for your task!", Toast.LENGTH_SHORT).show()
            taskReminder=false
        }else{
            if (taskReminder) {
                taskReminder = false
                dialogView.findViewById<Button>(R.id.task_date_selection_menu_setReminderButton).text =
                    "SET REMINDER"

            } else {
                taskReminder = true
                dialogView.findViewById<Button>(R.id.task_date_selection_menu_setReminderButton).text =
                    "CANCEL REMINDER"
            }
        }
    }

    private fun timePickerPopupMenu(dialogView: View) {
        var currentHour = ""
        var currentMinute = ""

        val timePicker: TimePickerDialog
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        timePicker = TimePickerDialog(context, { view, hourOfDay, minute ->
            currentHour = if (hourOfDay < 10) "0$hourOfDay" else hourOfDay.toString()
            currentMinute = if (minute < 10) "0$minute" else minute.toString()
            taskTime = currentHour + ":" + currentMinute
            dialogView.findViewById<Button>(R.id.task_date_selection_menu_setTimeButton).text =
                taskTime
        }, hour, minute, true)

        timePicker.setOnCancelListener {
            taskTime = ""
            dialogView.findViewById<Button>(R.id.task_date_selection_menu_setTimeButton).text =
                "SET TIME"
        }
        timePicker.show()

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.mainColor400, typedValue, true)
        timePicker.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(typedValue.data)
        timePicker.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(typedValue.data)
    }

    private fun updateTask() {
        job = CoroutineScope(Dispatchers.IO).launch {
            tasksDao.updateTask(
                taskName,
                taskDescription,
                taskDate,
                taskTime,
                taskPriority,
                isComplated,
                taskReminder,
                task.taskId
            )
        }
    }

    private fun scheduleNotification(taskId:Int) {
        val intent = Intent(requireContext(),Notification::class.java)
        val title = "Hey! It's time"
        val message = taskName
        intent.putExtra(titleExtra,title)
        intent.putExtra(messageExtra,message)

        val pendingIntent= PendingIntent.getBroadcast(
            requireContext(),
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager=activity?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val format = SimpleDateFormat("dd/MM/yyyy-hh:mm", Locale.getDefault())
        val time = format.parse("$taskDate-$taskTime")

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time.time,pendingIntent)
    }

    private fun cancelNotification(taskId: Int) {
        val intent = Intent(requireContext(), Notification::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel()
    }
}