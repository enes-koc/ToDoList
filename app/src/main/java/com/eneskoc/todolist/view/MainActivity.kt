package com.eneskoc.todolist.view

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.eneskoc.todolist.R
import com.eneskoc.todolist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(),ThemeChangeListener {

    private lateinit var binding: ActivityMainBinding
    private val PREFS_NAME = "options"
    private val THEME="theme"

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = sharedPreferences.getInt(THEME, R.style.Theme_MainTheme)
        setTheme(savedTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding?.let {

            setContentView(R.layout.activity_main)
            setContentView(binding.root)
            val view = binding.root
            setContentView(view)

            replaceFragment(ToDoListPage())

            binding.bottomNavigationView.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.toDoListRecyclerView -> replaceFragment(ToDoListPage())
                    R.id.calendar -> replaceFragment(CalendarPage())
                    R.id.charts -> replaceFragment(ChartsPage())
                    else -> {
                    }
                }
                true
            }
        }


    }

    fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    override fun onThemeChanged() {
        recreate()
    }
}

interface ThemeChangeListener {
    fun onThemeChanged()
}