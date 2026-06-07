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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.recipebytes.MealReminderReceiver
import com.example.recipebytes.PowerReceiver
import com.example.recipebytes.R
import com.example.recipebytes.fragments.ExploreFragment
import com.example.recipebytes.fragments.HomeFragment
import com.example.recipebytes.fragments.PlannerFragment
import com.example.recipebytes.fragments.ProfileFragment
import com.example.recipebytes.fragments.SuggestFragment
import com.example.recipebytes.preferences.UserPreferencesRepository
import com.example.recipebytes.services.FirebaseAuthService
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authService = FirebaseAuthService()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var powerReceiver: PowerReceiver
    private lateinit var preferencesRepository: UserPreferencesRepository
    private  var isDeepLinking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesRepository = UserPreferencesRepository(this)

        lifecycleScope.launch {
            preferencesRepository.isDarkModeFlow.collect { isDarkMode ->
                val nightMode = if (isDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        }

        val userId = intent.getStringExtra("userId")
            ?: authService.getCurrentUserId()
            ?: ""

        if (userId.isEmpty()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_done_$userId", false) ||
                prefs.getBoolean("onboarding_done", false)

        if (!onboardingDone) {
            val intent = Intent(this, OnboardingActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottom_nav_view)

        handleNotificationPermission()
        scheduleMealReminder()
        setupWindowInsets()
        setupBottomNavigation()
        val openFavorites = intent.getBooleanExtra("open_favorites", false)
        val openPlanner = intent.getBooleanExtra("open_planner", false)
        val openExplore = intent.getBooleanExtra("open_explore", false)
        isDeepLinking = false
        if (openFavorites) {
            intent.removeExtra("open_favorites")
            val frag = ExploreFragment().apply {
                arguments = Bundle().apply { putBoolean("show_favorites", true) }
            }
            isDeepLinking = true
            loadFragment(frag)
            bottomNav.selectedItemId = R.id.nav_explore
            isDeepLinking = false
            lifecycleScope.launch { preferencesRepository.setLastScreen("explore") }
            return  // ← STOP HERE, don't fall into lastScreen logic
        }

        if (openPlanner) {
            loadFragment(PlannerFragment())
            bottomNav.selectedItemId = R.id.nav_planner
            lifecycleScope.launch { preferencesRepository.setLastScreen("planner") }
            return
        }

        if (openExplore) {
            loadFragment(ExploreFragment())
            bottomNav.selectedItemId = R.id.nav_explore
            lifecycleScope.launch { preferencesRepository.setLastScreen("explore") }
            return
        }
//
////        // Handle deep-link navigation on cold start
////        if (intent.getBooleanExtra("open_planner", false)) {
////            loadFragment(PlannerFragment())
////            bottomNav.selectedItemId = R.id.nav_planner
////            lifecycleScope.launch { preferencesRepository.setLastScreen("planner") }
////        } else if (intent.getBooleanExtra("open_explore", false)) {
////            loadFragment(ExploreFragment())
////            bottomNav.selectedItemId = R.id.nav_explore
////            lifecycleScope.launch { preferencesRepository.setLastScreen("explore") }
////        } else if (intent.getBooleanExtra("open_favorites", false)) {
////            intent.removeExtra("open_favorites")
////            val frag = ExploreFragment()
////            frag.arguments = Bundle().apply { putBoolean("show_favorites", true) }
////            loadFragment(frag)
////            bottomNav.selectedItemId = R.id.nav_explore
////            lifecycleScope.launch { preferencesRepository.setLastScreen("explore") }
////        }
//      else
//
//
//        {
//            // Load last viewed screen
            lifecycleScope.launch {
                val lastScreen = preferencesRepository.lastScreenFlow.first() // ← read once, done

              ///  val lastScreen = preferencesRepository.lastScreenFlow.first() // ← read once, done


//                preferencesRepository.lastScreenFlow.collect { lastScreen ->
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    when (lastScreen) {
                        "explore" -> {
                            if (currentFragment !is ExploreFragment) {
                                loadFragment(ExploreFragment())
                                bottomNav.selectedItemId = R.id.nav_explore
                            }
                        }
                        "planner" -> {
                            if (currentFragment !is PlannerFragment) {
                                loadFragment(PlannerFragment())
                                bottomNav.selectedItemId = R.id.nav_planner
                            }
                        }
                        "profile" -> {
                            if (currentFragment !is ProfileFragment) {
                                loadFragment(ProfileFragment())
                                bottomNav.selectedItemId = R.id.nav_profile
                            }
                        }
                        "suggest" -> {
                            if (currentFragment !is SuggestFragment) {
                                loadFragment(SuggestFragment())
                                bottomNav.selectedItemId = R.id.nav_suggest
                            }
                        }
                        else -> {
                            if (currentFragment !is HomeFragment) {
                                loadFragment(HomeFragment())
                                bottomNav.selectedItemId = R.id.nav_home
                            }
                        }
                    }
                }
           /// }
       /// }

        supportFragmentManager.addOnBackStackChangedListener {
            syncBottomNav()
        }
    }

    // ── navigation

    fun navigateToTab(tabId: Int) {
        bottomNav.selectedItemId = tabId
    }

    private fun setupBottomNavigation() {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(Color.WHITE, 0x80FFFFFF.toInt())
        val colorStateList = ColorStateList(states, colors)

        bottomNav.itemIconTintList  = colorStateList
        bottomNav.itemTextColor     = colorStateList
        bottomNav.itemActiveIndicatorColor = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomNav.itemBackground    = null

        bottomNav.setOnItemSelectedListener { item ->
            if (isDeepLinking) return@setOnItemSelectedListener true  // ← skip if deep-linking
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (item.itemId) {
                R.id.nav_home -> {
                    if (currentFragment !is HomeFragment) {
                        loadFragment(HomeFragment())
                    }
                    lifecycleScope.launch { preferencesRepository.setLastScreen("home") }
                    true
                }
                R.id.nav_explore -> {
                    if (currentFragment !is ExploreFragment) {
                        val frag = ExploreFragment()
                        loadFragment(frag)
                    }
                    lifecycleScope.launch { preferencesRepository.setLastScreen("explore") }
                    true
                }
                R.id.nav_planner -> {
                    if (currentFragment !is PlannerFragment) {
                        loadFragment(PlannerFragment())
                    }
                    lifecycleScope.launch { preferencesRepository.setLastScreen("planner") }
                    true
                }
                R.id.nav_profile -> {
                    if (currentFragment !is ProfileFragment) {
                        loadFragment(ProfileFragment())
                    }
                    lifecycleScope.launch { preferencesRepository.setLastScreen("profile") }
                    true
                }
                R.id.nav_suggest -> {
                    if (currentFragment !is SuggestFragment) {
                        loadFragment(SuggestFragment())
                    }
                    lifecycleScope.launch { preferencesRepository.setLastScreen("suggest") }
                    true
                }
                else -> false
            }
        }

    }

    private fun syncBottomNav() {
        when (supportFragmentManager.findFragmentById(R.id.fragment_container)) {
            is HomeFragment      -> bottomNav.selectedItemId = R.id.nav_home
            is ExploreFragment   -> bottomNav.selectedItemId = R.id.nav_explore
            is PlannerFragment   -> bottomNav.selectedItemId = R.id.nav_planner
            is SuggestFragment   -> bottomNav.selectedItemId = R.id.nav_suggest
            is ProfileFragment   -> bottomNav.selectedItemId = R.id.nav_profile
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // ── permissions

    private fun handleNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
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

    // ── alarm

    private fun scheduleMealReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.example.recipebytes.MEAL_REMINDER")
        intent.setClass(this, MealReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 60 * 1000L
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms())
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    else
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                else ->
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    // ── receiver
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // ✅ ADDED — handle notification tap when app already open
        if (intent.getBooleanExtra("open_planner", false)) {
            loadFragment(PlannerFragment())
            bottomNav.selectedItemId = R.id.nav_planner
            return
        }
        if (intent.getBooleanExtra("open_explore", false)) {
            loadFragment(ExploreFragment())
            bottomNav.selectedItemId = R.id.nav_explore
            return
        }

        if (intent.getBooleanExtra("open_favorites", false)) {
            intent.removeExtra("open_favorites")
            val frag = ExploreFragment().apply {
                arguments = Bundle().apply { putBoolean("show_favorites", true) }
            }
            isDeepLinking = true
            loadFragment(frag)
            bottomNav.selectedItemId = R.id.nav_explore
            isDeepLinking = false
            lifecycleScope.launch { preferencesRepository.setLastScreen("explore") }
        }
    }

    override fun onStart() {
        super.onStart()
        powerReceiver = PowerReceiver()
        registerReceiver(powerReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(powerReceiver)
    }
}