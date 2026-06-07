package com.example.recipebytes.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.recipebytes.R
import com.example.recipebytes.adapters.OnboardingAdapter
import com.example.recipebytes.models.OnboardingPage

// Onboarding wizard shown on first app launch with ViewPager
class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var tvSkip: TextView
    private lateinit var dot0: View
    private lateinit var dot1: View
    private lateinit var dot2: View

    private val pages = listOf(
        OnboardingPage(
            titleText = "Welcome to\nRecipe Bytes",
            imageRes = R.drawable.image_cutting_board,
            descText = "A social recipe platform where you can discover, create, share recipes, and plan your meals."
        ),
        OnboardingPage(
            titleText = "Share & Explore\nRecipes",
            imageRes = R.drawable.image_recipe_card,
            descText = "Post recipes, explore creations, and save your favorites."
        ),
        OnboardingPage(
            titleText = "Smart Suggest &\nMeal Planner",
            imageRes = R.drawable.image_planner,
            descText = "Generate shopping lists, plan meals, and track nutrients — all in one place."
        )
    )

    // Sets up the ViewPager, dots indicator, and next/skip buttons
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        tvSkip = findViewById(R.id.tvSkip)
        dot0 = findViewById(R.id.dot0)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)

        val adapter = OnboardingAdapter(this, pages)
        viewPager.adapter = adapter

        updateDots(0)
        updateControls(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateControls(position)
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.size - 1) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        tvSkip.setOnClickListener { finishOnboarding() }
    }

    // Updates the dot indicators to reflect the current page
    private fun updateDots(activePosition: Int) {
        val dots = listOf(dot0, dot1, dot2)
        for (i in dots.indices) {
            dots[i].background = ContextCompat.getDrawable(
                this,
                if (i == activePosition) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    // Updates the button text and skip visibility based on the current page
    private fun updateControls(position: Int) {
        val isLast = position == pages.size - 1
        btnNext.text = if (isLast) "Get Started" else "Next"
        tvSkip.visibility = if (isLast) TextView.GONE else TextView.VISIBLE
    }

    // Marks onboarding as complete and navigates to the main activity
    private fun finishOnboarding() {
        val userId = intent.getStringExtra("userId") ?: ""
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        // Save onboarding completion for this specific user
        if (userId.isNotEmpty()) {
            prefs.edit()
                .putBoolean("onboarding_done_$userId", true)
                .apply()
        } else {
            // Fallback for non-signup flow
            prefs.edit()
                .putBoolean("onboarding_done", true)
                .apply()
        }

        val intent = Intent(this, MainActivity::class.java)
        if (userId.isNotEmpty()) {
            intent.putExtra("userId", userId)
        }
        startActivity(intent)
        finish()
    }
}
