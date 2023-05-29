package com.eneskoc.todolist.view

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.eneskoc.todolist.R
import com.eneskoc.todolist.databinding.FragmentNewTaskSheetBinding
import com.eneskoc.todolist.model.Tasks
import com.eneskoc.todolist.roomdb.TasksDao
import com.eneskoc.todolist.roomdb.TasksDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*
import java.lang.String
import java.text.SimpleDateFormat
import java.util.*
import kotlin.let


class NewTaskSheet : BottomSheetDialogFragment() {

    var onDismissListener: DialogInterface.OnDismissListener? = null
    private var _binding: FragmentNewTaskSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: TasksDatabase
    private lateinit var tasksDao: TasksDao
    private var job: Job? = null
    private lateinit var taskList: List<Tasks>

    private var taskName = ""
    private var taskDescription= ""
    private var taskDate = ""
    private var taskTime = ""
    private var taskPriority = 0
    private var isComplated = false
    private var taskReminder = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)
        createNotificationChannel()
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
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentNewTaskSheetBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskDate = SimpleDateFormat("dd/MM/yyyy").format(Date()).toString()

        binding.newTaskCalendarButton.setOnClickListener {
            datePickerPopupMenu()
        }

        binding.newTaskPriorityButton.setOnClickListener {
            priorityPopupMenu()
        }

        binding.newTaskAddButton.setOnClickListener {
            if (taskDate.length < 5) {
                taskDate = SimpleDateFormat("dd/M/yyyy").format(Date()).toString()
            }
            taskName = binding.taskName.text.toString()
            isComplated = false

            job = CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) {
                    val task = Tasks(taskName, taskDescription, taskDate, taskTime, taskPriority, isComplated, taskReminder)
                    tasksDao.insertTask(task)
                    taskList = tasksDao.getAll()
                }
                if (taskReminder) {
                    scheduleNotification(taskList.last().taskId)
                }
                this@NewTaskSheet.dismiss()
            }
        }
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
                })
            .setNegativeButton("CANCEL",
                DialogInterface.OnClickListener { dialog, id ->
                    getDialog()?.cancel()
                }).show()

        calendarView.minDate = Date().time
        calendarView
            .setOnDateChangeListener { view, year, month, dayOfMonth ->
                currentDay = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()
                currentYear = String.valueOf(year)
                val tempMonth = month + 1
                currentMonth = if (tempMonth < 10) "0$tempMonth" else tempMonth.toString()
            }

        dialogView.findViewById<Button>(R.id.task_date_selection_menu_setTimeButton)
            .setOnClickListener {
                timePickerPopupMenu(dialogView)
            }

        dialogView.findViewById<Button>(R.id.task_date_selection_menu_setReminderButton)
            .setOnClickListener {
                setReminder(dialogView)
            }

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.mainColor400, typedValue, true)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(typedValue.data)
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(typedValue.data)
    }

    private fun timePickerPopupMenu(dialogView: View) {
        var currentHour = ""
        var currentMinute = ""

        val timePicker: TimePickerDialog
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        timePicker = TimePickerDialog(context, { view, hourOfDay, minute ->
            //currentHour = hourOfDay.toString()
            //currentMinute = minute.toString()
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

    private fun priorityPopupMenu() {
        val popupMenu = PopupMenu(context, binding.newTaskPriorityButton)
        popupMenu.inflate(R.menu.priority_selection_menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val priority = when (menuItem.itemId) {
                R.id.noPriority -> {
                    binding.newTaskPriorityButton.setImageResource(R.drawable.prioirty_no_flag_24)
                    0
                }
                R.id.lowPriority -> {
                    binding.newTaskPriorityButton.setImageResource(R.drawable.priority_low_flag_24)
                    1
                }
                R.id.mediumPriority -> {
                    binding.newTaskPriorityButton.setImageResource(R.drawable.priority_medium_flag_24)
                    2
                }
                R.id.highPriority -> {
                    binding.newTaskPriorityButton.setImageResource(R.drawable.priority_high_flag_24)
                    3
                }
                else -> return@setOnMenuItemClickListener false
            }

            taskPriority = priority
            true
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true)
        }
        popupMenu.show()
    }

    private fun setReminder(dialogView: View){

        if (taskTime=="") {
            Toast.makeText(requireContext(),"You need to set a time for your task!",Toast.LENGTH_SHORT).show()
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

    private fun scheduleNotification(taskId:Int) {
        val intent = Intent(requireContext(),Notification::class.java)
        val title = "Hey! It's time"
        val message = taskName
        intent.putExtra(titleExtra,title)
        intent.putExtra(messageExtra,message)

        val pendingIntent=PendingIntent.getBroadcast(
            requireContext(),
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager=activity?.getSystemService(ALARM_SERVICE) as AlarmManager

        val format = SimpleDateFormat("dd/MM/yyyy-hh:mm", Locale.getDefault())
        val time = format.parse("$taskDate-$taskTime")

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time.time,pendingIntent)
    }


    private fun createNotificationChannel() {
        val name="Notify Channel"
        val desc="Turn on to receive notifications when your tasks are due."
        val importance=NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID,name,importance)
        channel.description=desc
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}