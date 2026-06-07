package com.example.recipebytes.activities

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.LikersAdapter
import com.example.recipebytes.adapters.MyRecipesAdapter
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.services.FirebaseAuthService
import com.example.recipebytes.services.FirebaseRecipeService

// Displays the current user's list of recipes
class MyRecipesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyRecipesAdapter
    private lateinit var tvNoRecipes: TextView
    private lateinit var layoutLoading: LinearLayout
    private val currentUserId = FirebaseAuthService().getCurrentUserId() ?: ""
    private var userProfileImageUrl = ""
    private val firebaseService = FirebaseRecipeService()

    // Initializes views and fetches the user profile on creation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_recipes)

        findViewById<TextView>(R.id.headerTitle).text = "My Recipes"

        recyclerView = findViewById(R.id.myRecipeRecyclerView)
        tvNoRecipes = findViewById(R.id.tvNoRecipes)
        layoutLoading = findViewById(R.id.layoutLoading)

        fetchUserProfile()
    }

    // Loads the current user's profile image from Firebase
    private fun fetchUserProfile() {
        val authService = FirebaseAuthService()
        authService.fetchUserFromDatabase(currentUserId,
            onSuccess = { user ->
                if (user != null) {
                    userProfileImageUrl = user.profileImage
                }
                setupAdapter()
            },
            onError = {
                setupAdapter()
            }
        )
    }

    // Sets up the RecyclerView adapter and loads the user's recipes
    private fun setupAdapter() {
        adapter = MyRecipesAdapter(
            mutableListOf(), currentUserId, userProfileImageUrl,
            onShowLikers = { recipeId -> showLikersDialog(recipeId) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadMyRecipes()
    }

    // Loads recipes owned by the current user from the repository
    private fun loadMyRecipes() {
        layoutLoading.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvNoRecipes.visibility = View.GONE
        
        RecipeRepository.loadFromFirebase {
            val allRecipes = RecipeRepository.getAllRecipes()
            val myRecipes = allRecipes.filter { it.userId == currentUserId }

            layoutLoading.visibility = View.GONE
            if (myRecipes.isEmpty()) {
                tvNoRecipes.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvNoRecipes.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.refresh(myRecipes)
            }
        }
    }

    // Shows a dialog listing users who liked a given recipe
    private fun showLikersDialog(recipeId: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_likers)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val ivClose = dialog.findViewById<ImageView>(R.id.ivCloseDialog)
        val rvLikers = dialog.findViewById<RecyclerView>(R.id.rvLikers)

        rvLikers.layoutManager = LinearLayoutManager(this)

        firebaseService.getLikedByUsers(recipeId) { likedByMap ->
            val adapter = LikersAdapter(this, likedByMap)
            rvLikers.adapter = adapter
        }

        ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
