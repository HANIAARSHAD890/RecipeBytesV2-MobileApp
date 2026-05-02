package com.example.recipebytes.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.example.recipebytes.MealReminderReceiver
import android.os.Build
import android.content.IntentFilter
import com.example.recipebytes.PowerReceiver
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.recipebytes.R
import com.example.recipebytes.fragments.ExploreFragment
import com.example.recipebytes.fragments.HomeFragment
import com.example.recipebytes.fragments.PlannerFragment
import com.example.recipebytes.fragments.SuggestFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main activity that manages the bottom navigation and fragment switching.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var powerReceiver: PowerReceiver
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleNotificationPermission()
        scheduleMealReminder()
        setupWindowInsets()
        setupBottomNavigation()

        loadFragment(HomeFragment())

        supportFragmentManager.addOnBackStackChangedListener {
            syncBottomNav()
        }
    }

    /**
     * Handles notification permission request for Android 13+.
     */
    private fun handleNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
                )
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Initializes the bottom navigation view and its selection listener.

    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_nav_view)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(Color.WHITE, 0x80FFFFFF.toInt())
        val colorStateList = ColorStateList(states, colors)
        bottomNav.itemIconTintList = colorStateList
        bottomNav.itemTextColor = colorStateList
        bottomNav.itemActiveIndicatorColor = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomNav.itemBackground = null

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_explore -> { loadFragment(ExploreFragment()); true }
                R.id.nav_planner -> { loadFragment(PlannerFragment()); true }
                R.id.nav_suggest -> { loadFragment(SuggestFragment()); true }
                else -> false
            }
        }
    }

    /**
     * Synchronizes the bottom navigation selection with the current fragment in the container.
     */
    private fun syncBottomNav() {
        when (supportFragmentManager.findFragmentById(R.id.fragment_container)) {
            is HomeFragment -> bottomNav.selectedItemId = R.id.nav_home
            is ExploreFragment -> bottomNav.selectedItemId = R.id.nav_explore
            is PlannerFragment -> bottomNav.selectedItemId = R.id.nav_planner
            is SuggestFragment -> bottomNav.selectedItemId = R.id.nav_suggest
        }
    }

    private fun scheduleMealReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.example.recipebytes.MEAL_REMINDER")
        intent.setClass(this, MealReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 60 * 1000L

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires explicit permission check
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback if permission denied
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        syncBottomNav()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    override fun onStart() {
        super.onStart()
        powerReceiver = PowerReceiver()
        val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        registerReceiver(powerReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(powerReceiver)
    }
}
