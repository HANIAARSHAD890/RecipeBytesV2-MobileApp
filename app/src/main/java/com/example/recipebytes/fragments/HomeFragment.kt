package com.example.recipebytes.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.activities.MainActivity
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.services.FirebaseAuthService
import com.example.recipebytes.services.FirebaseRecipeService

class HomeFragment : Fragment() {

    private val authService = FirebaseAuthService()
    private val recipeService = FirebaseRecipeService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardClickListeners(view)
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

    private fun setupProfileSection(view: View) {
        view.findViewById<View>(R.id.btnViewProfile).setOnClickListener {
            navigateToTab(R.id.nav_profile)
        }
    }

    private fun loadUserData(view: View) {
        val userId = authService.getCurrentUserId() ?: ""
        if (userId.isNotEmpty()) {
            // Load user profile data
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

            // Load dashboard metrics
            loadDashboardMetrics(view, userId)
        } else {
            view.findViewById<TextView>(R.id.tvUsername).text = "Aunt_Sallys_Kitchen"
        }
    }

    private fun loadDashboardMetrics(view: View, userId: String) {
        // Use RecipeRepository (same data source as Explore) instead of direct Firebase query
        RecipeRepository.loadFromFirebase {
            if (!isAdded) return@loadFromFirebase
            
            try {
                val allRecipes = RecipeRepository.getAllRecipes()
                val userRecipes = allRecipes.filter { it.userId == userId }
                var total = 0
                var pub = 0
                var priv = 0
                var likes = 0
                for (r in userRecipes) {
                    total++
                    if (r.isPublic) pub++ else priv++
                    likes += r.likesCount
                }
                
                view.findViewById<TextView>(R.id.tvTotalRecipes)?.text = total.toString()
                view.findViewById<TextView>(R.id.tvPublicCount)?.text = pub.toString()
                view.findViewById<TextView>(R.id.tvPrivateCount)?.text = priv.toString()
                view.findViewById<TextView>(R.id.tvTotalLikes)?.text = likes.toString()
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error loading dashboard metrics", e)
                // Set defaults on error
                view.findViewById<TextView>(R.id.tvTotalRecipes)?.text = "0"
                view.findViewById<TextView>(R.id.tvPublicCount)?.text = "0"
                view.findViewById<TextView>(R.id.tvPrivateCount)?.text = "0"
                view.findViewById<TextView>(R.id.tvTotalLikes)?.text = "0"
            }
        }
    }
}
