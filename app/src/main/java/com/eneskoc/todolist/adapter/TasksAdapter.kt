package com.eneskoc.todolist.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eneskoc.todolist.R
import com.eneskoc.todolist.databinding.ToDoListRecyclerRowBinding
import com.eneskoc.todolist.model.Tasks
import com.eneskoc.todolist.view.TaskEditPage
import com.google.android.material.internal.ContextUtils.getActivity
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter(
    var tasksList: List<Tasks>,
    private val onItemClickListener: OnItemClickListener?
) : RecyclerView.Adapter<TasksAdapter.TasksHolder>() {

    class TasksHolder(
        val toDoListRecyclerRowBinding: ToDoListRecyclerRowBinding,
        onItemClickListener: OnItemClickListener?
    ) : RecyclerView.ViewHolder(toDoListRecyclerRowBinding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksHolder {
        val recyclerRowBinding =
            ToDoListRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TasksHolder(recyclerRowBinding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: TasksHolder, position: Int) {
        val typedValue = TypedValue()
        holder.itemView.context.theme.resolveAttribute(R.attr.mainColor100, typedValue, true)
        val backgroundColor = typedValue.data

        if (tasksList.get(position).taskReminder) {
            holder.toDoListRecyclerRowBinding.recyclerViewReminderImageView.visibility =
                View.VISIBLE
        } else {
            holder.toDoListRecyclerRowBinding.recyclerViewReminderImageView.visibility = View.GONE
        }

        if (tasksList.get(position).isCompleted) {
            holder.toDoListRecyclerRowBinding.recyclerViewTaskIsComplated.isChecked = true
            holder.toDoListRecyclerRowBinding.recyclerViewTaskName.apply {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }

            holder.toDoListRecyclerRowBinding.recyclerViewRow.setBackgroundColor(backgroundColor)
            if (tasksList.get(tasksList.indexOfFirst { it.isCompleted }) == tasksList.get(position)) {
                holder.toDoListRecyclerRowBinding.recyclerViewTaskHeader.text = "-Completed"
                holder.toDoListRecyclerRowBinding.recyclerViewTaskHeader.visibility = View.VISIBLE
            }
        } else {
            holder.toDoListRecyclerRowBinding.recyclerViewTaskIsComplated.isChecked = false
            holder.toDoListRecyclerRowBinding.recyclerViewTaskName.apply {
                paintFlags = 0
            }
            if (tasksList.get(tasksList.indexOfFirst { !it.isCompleted }) == tasksList.get(position)) {
                holder.toDoListRecyclerRowBinding.recyclerViewTaskHeader.text = "-Ongoing tasks"
                holder.toDoListRecyclerRowBinding.recyclerViewTaskHeader.visibility = View.VISIBLE
            }

        }

        holder.toDoListRecyclerRowBinding.recyclerViewTaskName.text =
            tasksList.get(position).taskName
        holder.toDoListRecyclerRowBinding.recyclerViewAlarmDateTextView.text =
            dateConvertString(tasksList.get(position).taskDate)
        when (tasksList.get(position).taskPriority) {
            0 -> {
                holder.toDoListRecyclerRowBinding.recyclerViewPriorityImageView.setImageResource(R.drawable.prioirty_no_flag_24)
            }
            1 -> {
                holder.toDoListRecyclerRowBinding.recyclerViewPriorityImageView.setImageResource(R.drawable.priority_low_flag_24)
            }
            2 -> {
                holder.toDoListRecyclerRowBinding.recyclerViewPriorityImageView.setImageResource(R.drawable.priority_medium_flag_24)
            }
            3 -> {
                holder.toDoListRecyclerRowBinding.recyclerViewPriorityImageView.setImageResource(R.drawable.priority_high_flag_24)
            }
        }

        holder.toDoListRecyclerRowBinding.recyclerViewTaskIsComplated.setOnClickListener {
            if (holder.toDoListRecyclerRowBinding.recyclerViewTaskIsComplated.isChecked) {
                onItemClickListener?.onClick(tasksList.get(position))
            } else {
                onItemClickListener?.onClick(tasksList.get(position))
            }
        }
        holder.itemView.setOnClickListener {
            val fragmentManager =
                (holder.itemView.context as AppCompatActivity).supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.frame_layout, TaskEditPage(tasksList.get(position)))
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

    }

    interface OnItemClickListener {
        fun onClick(task: Tasks)
    }

    override fun getItemCount(): Int {
        return tasksList.size
    }

    private fun dateConvertString(date: String): String {
        val date = date
        val inputFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val newDate = inputFormat.parse(date)
        val outputDateString = newDate?.let { outputFormat.format(it) }
        return outputDateString.toString()
    }

    fun updateData(newData: List<Tasks>) {
        tasksList = newData
        notifyDataSetChanged()
    }
}

