package com.example.recipebytes.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Home screen fragment act as DASHBOARD for the user.
 */
class HomeFragment : Fragment() {

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
    }

    /**
     * Configures click listeners for the navigation cards on the home screen.
     */
    private fun setupCardClickListeners(view: View) {
        view.findViewById<CardView>(R.id.card_recepies).setOnClickListener {
            //navigateTo(ExploreFragment(), R.id.nav_explore)
        }
        view.findViewById<CardView>(R.id.card_mealplanner).setOnClickListener {
            //navigateTo(PlannerFragment(), R.id.nav_planner)
        }
        view.findViewById<CardView>(R.id.card_smartsuggest).setOnClickListener {
            //navigateTo(SuggestFragment(), R.id.nav_suggest)
        }
        view.findViewById<CardView>(R.id.card_add_recipe).setOnClickListener {
            startActivity(Intent(requireContext(), AddRecipeActivity::class.java))
        }
    }

    /**
     * Sets up the theme toggle button to switch between light and dark modes.
     */
    private fun setupThemeToggle(view: View) {
        view.findViewById<ImageView>(R.id.btnTheme).setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
           // updateSystemBarAppearance()
        }
    }

    /**
     * Updates the system bar appearance for light/dark mode compatibility.
     */
//    private fun updateSystemBarAppearance() {
//        requireActivity().window.insetsController?.let {
//            it.setSystemBarsAppearance(
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
//                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
//                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
//                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
//            )
//        }
//    }

    /**
     * Helper to handle fragment transactions and bottom navigation state synchronization.
     */
//    private fun navigateTo(fragment: Fragment, navItemId: Int) {
//        parentFragmentManager.beginTransaction()
//            .replace(R.id.fragment_container, fragment)
//            .addToBackStack(null)
//            .commit()
//
//        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
//        bottomNav.selectedItemId = navItemId
//    }
}
