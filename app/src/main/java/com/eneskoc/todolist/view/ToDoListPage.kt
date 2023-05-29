package com.eneskoc.todolist.view

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat.recreate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.eneskoc.todolist.R
import com.eneskoc.todolist.adapter.TasksAdapter
import com.eneskoc.todolist.databinding.FragmentToDoListBinding
import com.eneskoc.todolist.model.Tasks
import com.eneskoc.todolist.roomdb.TasksDao
import com.eneskoc.todolist.roomdb.TasksDatabase
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class ToDoListPage : Fragment(), TasksAdapter.OnItemClickListener {

    private val PREFS_NAME = "options"
    private val COMPLATED_FILTER = "complatedFilter"
    private val THEME = "theme"

    private lateinit var sharedPreferences: SharedPreferences

    private var _binding: FragmentToDoListBinding? = null
    private val binding get() = _binding!!

    private lateinit var onItemClickListener: TasksAdapter.OnItemClickListener

    private lateinit var db: TasksDatabase
    private lateinit var tasksDao: TasksDao
    private var job: Job? = null

    private lateinit var taskList: List<Tasks>
    private lateinit var tasksAdapter: TasksAdapter
    var showCompleted = true


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
        _binding = FragmentToDoListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toDoListRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
        onItemClickListener = this
        getData()

        //Kullanıcının belirlediği ayarlar alınıyor.
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val complatedFilterOptions = sharedPreferences.getBoolean(COMPLATED_FILTER, true)
        showCompleted = complatedFilterOptions

        //Başlık için bugünün tarihi alınıp yazılıyor.
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
        val formattedDate = currentDate.format(formatter)
        binding.todayTextView.text = formattedDate

        binding.addNewTaskButton.setOnClickListener {
            val newTaskSheet = NewTaskSheet()
            newTaskSheet.onDismissListener = object : DialogInterface.OnDismissListener {
                override fun onDismiss(dialog: DialogInterface?) {
                    newTaskSheetIsClose()
                }
            }
            newTaskSheet.show(parentFragmentManager, newTaskSheet.tag)
        }

        binding.optionsMenu.setOnClickListener {
            optionsPopupMenu()
        }

        //Item swipe yapılınca siliniyor. Reminder varsa reminder çalışmaması için o da siliniyor.
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_delete_sweep_24)
                drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                drawable?.draw(c)

                if (dX > 0) {
                    val backgroundColor = ContextCompat.getColor(requireContext(), R.color.priorityColorRed)

                    val itemView = viewHolder.itemView
                    val itemHeight = itemView.bottom - itemView.top

                    c.drawRect(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.left + dX,
                        itemView.bottom.toFloat(),
                        Paint().apply {
                            color = backgroundColor
                        }
                    )

                    val translateX = dX * 0.95f
                    c.save()
                    c.translate(
                        itemView.left + translateX - drawable!!.intrinsicWidth,
                        itemView.top + (itemHeight - drawable!!.intrinsicHeight) / 2f
                    )
                    if (drawable != null) {
                        drawable.draw(c)
                    }
                    c.restore()
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletedTask: Tasks = taskList[viewHolder.absoluteAdapterPosition]
                job = CoroutineScope(Dispatchers.IO).launch {
                    tasksDao.deleteTask(deletedTask)
                    withContext(Dispatchers.Main) {
                        getData()
                    }

                    if (deletedTask.taskReminder){
                        cancelNotification(deletedTask.taskId)
                    }
                }

                Snackbar.make(
                    binding.toDoListRecyclerView,
                    "Deleted " + deletedTask.taskName,
                    Snackbar.LENGTH_LONG
                ).setAction(
                    "Undo"
                ) {
                    job = CoroutineScope(Dispatchers.IO).launch {
                        tasksDao.insertTask(deletedTask)
                        withContext(Dispatchers.Main) {
                            getData()
                        }
                    }
                }.show()
            }
        }).attachToRecyclerView(binding.toDoListRecyclerView)
    }


    private fun getData() {
        job?.cancel()

        job = CoroutineScope(Dispatchers.IO).launch {
            taskList = tasksDao.getAll().run {
                if (!showCompleted) filter { !it.isCompleted } else this
            }
                .sortedWith(compareBy<Tasks> { it.isCompleted }.thenBy {
                    LocalDate.parse(
                        it.taskDate,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    )
                })
            withContext(Dispatchers.Main) {
                tasksAdapter = TasksAdapter(taskList, onItemClickListener)
                binding.toDoListRecyclerView.adapter = tasksAdapter
                tasksAdapter.updateData(taskList)
                binding.toDoListRecyclerView.scrollToPosition(taskList.indexOfFirst { it.isCompleted } - 1)
            }
        }
    }

    private fun updateTaskIsComplateStatus(tasks: Tasks) {
        job?.cancel() // Önceki işlemi iptal et

        job = CoroutineScope(Dispatchers.IO).launch {
            val newCompletionStatus = !tasks.isCompleted
            tasksDao.updateTaskCompletionStatus(tasks.taskId, newCompletionStatus)

            withContext(Dispatchers.Main) {
                getData()
            }
        }
    }

    private fun optionsPopupMenu() {
        val popupMenu = PopupMenu(context, binding.optionsMenu)
        popupMenu.inflate(R.menu.options_menu)

        val complatedTaskFilter = popupMenu.menu.findItem(R.id.complatedFilter)
        complatedTaskFilter.title = if (showCompleted) "Hide Completed" else "Show Completed"

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.complatedFilter -> {
                    showCompleted = !showCompleted
                    sharedPreferences.edit().putBoolean(COMPLATED_FILTER, showCompleted).apply()
                    getData()
                    true
                }
                R.id.changeTheme -> {
                    themePopupMenu()
                    true
                }
                else -> true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true)
        }
        popupMenu.show()
    }

    private fun themePopupMenu() {
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val themeOptions = listOf(
            Pair(R.id.themeOptionsMain, R.style.Theme_MainTheme),
            Pair(R.id.themeOptionsWinter, R.style.Theme_WinterTheme),
            Pair(R.id.themeOptionsSpring, R.style.Theme_SpringTheme),
            Pair(R.id.themeOptionsSummer, R.style.Theme_SummerTheme),
            Pair(R.id.themeOptionsAutumn, R.style.Theme_AutumnTheme)
        )

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.theme_selection_menu, null)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
            .show()

        val themeChangeListener = activity as? ThemeChangeListener

        themeOptions.forEach { (optionId, themeStyleResId) ->
            dialogView.findViewById<ImageView>(optionId).setOnClickListener {
                sharedPreferences.edit().putInt(THEME, themeStyleResId).apply()
                themeChangeListener?.onThemeChanged()
                dialog.dismiss()
            }
        }

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.mainColor400, typedValue, true)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(typedValue.data)
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(typedValue.data)
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

    private fun newTaskSheetIsClose() {
        getData()
    }

    override fun onResume() {
        super.onResume()
        getData()
    }

    override fun onPause() {
        super.onPause()
        job?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel()
    }

    override fun onClick(tasks: Tasks) {
        updateTaskIsComplateStatus(tasks)
    }
}
