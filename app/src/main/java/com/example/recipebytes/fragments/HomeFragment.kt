package com.example.recipebytes.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.activities.MainActivity
import com.example.recipebytes.services.FirebaseAuthService

class HomeFragment : Fragment() {

    private val authService = FirebaseAuthService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardClickListeners(view)
        setupThemeToggle(view)
        setupProfileSection(view)

        loadUserData(view)
    }

    private fun setupCardClickListeners(view: View) {
        view.findViewById<CardView>(R.id.card_recepies).setOnClickListener {
            navigateToTab(R.id.nav_explore)
        }
        view.findViewById<CardView>(R.id.card_mealplanner).setOnClickListener {
            navigateToTab(R.id.nav_planner)
        }
        view.findViewById<CardView>(R.id.card_smartsuggest).setOnClickListener {
            navigateToTab(R.id.nav_suggest)
        }
        view.findViewById<CardView>(R.id.card_add_recipe).setOnClickListener {
            startActivity(Intent(requireContext(), AddRecipeActivity::class.java))
        }
    }

    private fun navigateToTab(tabId: Int) {
        val activity = requireActivity() as? MainActivity ?: return
        activity.navigateToTab(tabId)
    }

    private fun setupThemeToggle(view: View) {
        view.findViewById<ImageView>(R.id.btnTheme).setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private fun setupProfileSection(view: View) {
        view.findViewById<View>(R.id.btnViewProfile).setOnClickListener {
            navigateToTab(R.id.nav_profile)
        }
    }

    private fun loadUserData(view: View) {
        val userId = authService.getCurrentUserId() ?: ""
        if (userId.isNotEmpty()) {
            authService.fetchUserFromDatabase(userId,
                onSuccess = { user ->
                    val displayName = if (user?.username?.isNotEmpty() == true) user.username
                        else if (user?.email?.contains("@") == true) user.email.substringBefore("@")
                        else "Aunt_Sallys_Kitchen"
                    view.findViewById<TextView>(R.id.tvUsername).text = displayName
                },
                onError = {
                    view.findViewById<TextView>(R.id.tvUsername).text = "Aunt_Sallys_Kitchen"
                }
            )
        } else {
            view.findViewById<TextView>(R.id.tvUsername).text = "Aunt_Sallys_Kitchen"
        }
    }
}
