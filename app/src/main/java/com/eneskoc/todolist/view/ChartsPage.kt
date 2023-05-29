package com.eneskoc.todolist.view

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.room.Room
import com.eneskoc.todolist.R
import com.eneskoc.todolist.databinding.FragmentChartsPageBinding
import com.eneskoc.todolist.model.Tasks
import com.eneskoc.todolist.roomdb.TasksDao
import com.eneskoc.todolist.roomdb.TasksDatabase
import com.github.aachartmodel.aainfographics.aachartcreator.*
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AADataLabels
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


class ChartsPage : Fragment() {

    private var _binding: FragmentChartsPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: TasksDatabase
    private lateinit var tasksDao: TasksDao
    private var job: Job? = null

    private lateinit var taskList: List<Tasks>

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
        _binding = FragmentChartsPageBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    val calendar = Calendar.getInstance()
    var currentYear = calendar.get(Calendar.YEAR)
    var currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

    private lateinit var aaWeekChartModel: AAChartModel
    lateinit var completedTasksArray: Array<Any>
    lateinit var ongoingTasksArray: Array<Any>
    var taskTitle = ""
    lateinit var xAxisWeeks : Array<String>


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshAllData()

        binding.totalTasksText.text = taskList.size.toString()
        binding.complatedTaksText.text = countOngoingOrComplatedTasks(taskList,true).toString()
        binding.ongoingTasksText.text = countOngoingOrComplatedTasks(taskList,false).toString()

        val progressBarValue = (countOngoingOrComplatedTasks(taskList,true).toFloat() / taskList.size.toFloat() * 100).toInt()
        binding.progressBar.progress = progressBarValue
        binding.progressBarText.text = "% $progressBarValue"

        aaWeekChartModel = createAAWeekChartModel()

        binding.aaWeekChartView.aa_drawChartWithChartModel(aaWeekChartModel)
        getTaskCountForToday(taskList)

        binding.chartNextWeek.setOnClickListener {
            currentWeek++
            refreshAllData()
            aaWeekChartModel = createAAWeekChartModel()
            binding.aaWeekChartView.aa_drawChartWithChartModel(aaWeekChartModel)
        }

        binding.chartPrevWeek.setOnClickListener {
            currentWeek--
            refreshAllData()
            aaWeekChartModel = createAAWeekChartModel()
            binding.aaWeekChartView.aa_drawChartWithChartModel(aaWeekChartModel)
        }


    }

    private fun createAAWeekChartModel(): AAChartModel {

        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Areaspline)
            .stacking(AAChartStackingType.Normal)
            .title(taskTitle)
            .yAxisTitle("Task Count")
            .yAxisMin(0)
            .yAxisAllowDecimals(true)
            .yAxisLabelsEnabled(true)
            .stacking(AAChartStackingType.Normal)
            .markerSymbolStyle(AAChartSymbolStyleType.InnerBlank)
            .backgroundColor(getHexColor(requireContext(), R.attr.mainColor100))
            .axesTextColor(getHexColor(requireContext(), R.attr.textColorLight900))
            .colorsTheme(arrayOf(getHexColor(requireContext(), R.attr.mainColor300),getHexColor(requireContext(), R.attr.mainColor200)))
            .categories(xAxisWeeks)
            .series(arrayOf(
                AASeriesElement()
                    .name("Complated")
                    .connectNulls(false)
                    .data(completedTasksArray),
                AASeriesElement()
                    .name("Ongoing")
                    .connectNulls(false)
                    .data(ongoingTasksArray)
            ))

        return aaChartModel
    }

    fun getTaskCountForToday(taskList: List<Tasks>) {
        val today = LocalDate.now()
        var taskCount = 0
        var complatedTaskCount=0

        taskList.forEach {
            val taskDate=LocalDate.parse(it.taskDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            if (taskDate == today) {
                taskCount++
                if(it.isCompleted){complatedTaskCount++}
            }
        }

        if (taskCount==0){
            binding.todayProgressBarRelativeLayout.visibility=View.GONE
            binding.todayTaskText.text= "Today is your rest day!"
        }else{
            binding.todayProgressBarRelativeLayout.visibility=View.VISIBLE
            binding.todayProgressBar.max=taskCount
            binding.todayProgressBar.progress=complatedTaskCount
            binding.todayProgressBarText.text="$complatedTaskCount/$taskCount"

            if (complatedTaskCount==0){
                binding.todayTaskText.text= "You haven't done any tasks yet."
            }
            else if(complatedTaskCount==taskCount){
                binding.todayTaskText.text= "You have completed all of your tasks."
            }
            else {
                binding.todayTaskText.text= "You did $complatedTaskCount of your tasks today."
            }
        }
    }

    fun getHexColor(context: Context, colorAttr: Int): String {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, typedValue, true)
        val color = typedValue.data
        return String.format("#%06X", 0xFFFFFF and color)
    }

    fun refreshAllData(){
        completedTasksArray= countCompletedTaskDatesInWeek(taskList,getDaysOfWeekInYear(currentYear,currentWeek)).map { it as Any }.toTypedArray()
        ongoingTasksArray = countOngoingTaskDatesInWeek(taskList,getDaysOfWeekInYear(currentYear,currentWeek)).map { it as Any }.toTypedArray()
        taskTitle = getFirstAndLastDaysOfWeekInYear(currentYear,currentWeek)
        xAxisWeeks = getOnlyDaysOfWeekInYear(currentYear,currentWeek)
    }

    //Başlık için seçili haftanın ilk ve son günlerini ayları ile birlikte verir. 29 May - 04 Jun gibi
    fun getFirstAndLastDaysOfWeekInYear(year: Int, week: Int): String {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.YEAR, year)
            set(Calendar.WEEK_OF_YEAR, week+1)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val firstDayOfWeek = calendar.time
        val lastDayOfWeek = calendar.apply { add(Calendar.DAY_OF_WEEK, 6) }.time

        val dateFormat = SimpleDateFormat("dd MMM")
        val formattedFirstDay = dateFormat.format(firstDayOfWeek)
        val formattedLastDay = dateFormat.format(lastDayOfWeek)

        return "$formattedFirstDay - $formattedLastDay"
    }

    //Seçili yıla ve haftaya göre tarihleri verir. "12/05/2023" gibi.
    fun getDaysOfWeekInYear(year: Int, week: Int): Array<String> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.YEAR, year)
            set(Calendar.WEEK_OF_YEAR, week+1)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        return Array(7) {
            SimpleDateFormat("dd/MM/yyyy").format(calendar.time).toString()
                .also { calendar.add(Calendar.DAY_OF_WEEK, 1) }
        }
    }

    //Seçili yıla ve haftaya göre sadece gün tarihlerini verir. "12,13,...17,18" gibi.
    fun getOnlyDaysOfWeekInYear(year: Int, week: Int): Array<String> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.YEAR, year)
            set(Calendar.WEEK_OF_YEAR, week+1)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        return Array(7) {
            SimpleDateFormat("dd").format(calendar.time).toString()
                .also { calendar.add(Calendar.DAY_OF_WEEK, 1) }
        }
    }

    //Verilen hafta aralığına göre tamamlanmış tasklarin sayılarını verir. "1,5,4,2,3,4,6" gibi.
    fun countCompletedTaskDatesInWeek(taskList: List<Tasks>, dates: Array<String>): Array<Int> {
        val result = Array(7) { 0 }

        dates.forEachIndexed { index, date ->
            taskList.forEach { task ->
                if (date == task.taskDate && task.isCompleted) {
                    result[index]++
                }
            }
        }
        return result
    }

    //Verilen hafta aralığına göre devam eden tasklarin sayılarını verir. "1,5,4,2,3,4,6" gibi.
    fun countOngoingTaskDatesInWeek(taskList: List<Tasks>, dates: Array<String>): Array<Int> {
        val result = Array(7) { 0 }

        dates.forEachIndexed { index, date ->
            taskList.forEach { task ->
                if (date == task.taskDate && !task.isCompleted) {
                    result[index]++
                }
            }
        }
        return result
    }

    //Tamamlanmış veya devam eden task saysını verir.
    fun countOngoingOrComplatedTasks(taskList: List<Tasks>, complated:Boolean): Int {
        var completedTaskCount = 0
        var ongoingTaskCount = 0

        taskList.forEach {
            if (it.isCompleted) {completedTaskCount++}
            else {ongoingTaskCount++}
        }

        if (complated){return completedTaskCount}
        else{return ongoingTaskCount}
    }

    private fun getData() {
        job = CoroutineScope(Dispatchers.IO).launch {
            taskList = tasksDao.getAll()
        }
    }

}