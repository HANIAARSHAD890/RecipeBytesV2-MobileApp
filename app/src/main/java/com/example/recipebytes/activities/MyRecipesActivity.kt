package com.example.recipebytes.activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.MyRecipesAdapter
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.services.FirebaseAuthService

class MyRecipesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyRecipesAdapter
    private lateinit var tvNoRecipes: TextView
    private lateinit var layoutLoading: LinearLayout
    private val currentUserId = FirebaseAuthService().getCurrentUserId() ?: ""
    private var userProfileImageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_recipes)

        findViewById<TextView>(R.id.headerTitle).text = "My Recipes"

        recyclerView = findViewById(R.id.myRecipeRecyclerView)
        tvNoRecipes = findViewById(R.id.tvNoRecipes)
        layoutLoading = findViewById(R.id.layoutLoading)

        fetchUserProfile()
    }

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

    private fun setupAdapter() {
        adapter = MyRecipesAdapter(mutableListOf(), currentUserId, userProfileImageUrl)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadMyRecipes()
    }

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
}
