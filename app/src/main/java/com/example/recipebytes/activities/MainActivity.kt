package com.example.recipebytes.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.recipebytes.MealReminderReceiver
import com.example.recipebytes.PowerReceiver
import com.example.recipebytes.R
import com.example.recipebytes.activities.SignInActivity
import com.example.recipebytes.fragments.ExploreFragment
import com.example.recipebytes.fragments.HomeFragment
import com.example.recipebytes.fragments.PlannerFragment
import com.example.recipebytes.fragments.ProfileFragment
import com.example.recipebytes.fragments.SuggestFragment
import com.example.recipebytes.models.User
import com.example.recipebytes.services.FirebaseAuthService
import com.example.recipebytes.services.RecipeSeeder
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // ===== Firebase Auth =====
    private val authService = FirebaseAuthService()

    // ===== UI / Navigation =====
    private lateinit var bottomNav: BottomNavigationView

    // ===== Receiver =====
    private lateinit var powerReceiver: PowerReceiver

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottom_nav_view)

        handleNotificationPermission()
        scheduleMealReminder()
        setupWindowInsets()
        setupBottomNavigation()

        // ===== USER LOGIN HANDLING =====
        val userId = intent.getStringExtra("userId")
            ?: authService.getCurrentUserId()
            ?: ""

        if (userId.isEmpty()) {
            navigateToSignIn()
            return
        }


        // Default fragment
        loadFragment(HomeFragment())

        supportFragmentManager.addOnBackStackChangedListener {
            syncBottomNav()
        }
    }

    private fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    // ================= NAVIGATION =================

    private fun setupBottomNavigation() {
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
                R.id.nav_home -> {
                    loadFragment(HomeFragment()); true
                }
                R.id.nav_explore -> {
                    loadFragment(ExploreFragment()); true
                }
                R.id.nav_planner -> {
                    loadFragment(PlannerFragment()); true
                }
                R.id.nav_suggest -> {
                    loadFragment(SuggestFragment()); true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun syncBottomNav() {
        when (supportFragmentManager.findFragmentById(R.id.fragment_container)) {
            is HomeFragment -> bottomNav.selectedItemId = R.id.nav_home
            is ExploreFragment -> bottomNav.selectedItemId = R.id.nav_explore
            is PlannerFragment -> bottomNav.selectedItemId = R.id.nav_planner
            is SuggestFragment -> bottomNav.selectedItemId = R.id.nav_suggest
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // ================= SYSTEM / PERMISSIONS =================

    private fun handleNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
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

    // ================= ALARM =================

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
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }

                else -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    // ================= RECEIVER =================

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