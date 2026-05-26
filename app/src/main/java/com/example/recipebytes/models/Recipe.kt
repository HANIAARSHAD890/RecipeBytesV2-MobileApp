package com.example.recipebytes.models

import android.content.Context
import com.example.recipebytes.services.FirebaseRecipeService
import java.io.Serializable

data class Recipe(
    var title: String,
    var description: String,
    var category: String,
    val imageUri: String? = null,
    val cookingTime: Int = 0,
    val servings: Int = 1,
    var isPublic: Boolean = true,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val nutrition: Nutrition? = null,
    val commentsCount: Int = 0,
    val likesCount: Int = 0,
    var recipeId: String = ""
) : Serializable

object RecipeRepository {

    private var recipes = mutableListOf<Recipe>()
    private var recipeIdMap = mutableMapOf<String, String>()
    private val firebaseService = FirebaseRecipeService()

    fun init(context: android.content.Context) {
        // Intentionally empty - recipes load from Firebase via loadFromFirebase()
    }

    fun loadFromFirebase(onComplete: () -> Unit = {}) {
        firebaseService.getAllRecipes(
            onSuccess = { fetched ->
                recipes.clear()
                recipeIdMap.clear()
                for ((key, recipe) in fetched) {
                    recipe.recipeId = key
                    recipes.add(recipe)
                    recipeIdMap[recipe.title] = key
                }
                onComplete()
            },
            onError = {
                onComplete()
            }
        )
    }

    fun addRecipe(context: android.content.Context, recipe: Recipe): Boolean {
        if (recipes.any { it.title == recipe.title }) {
            return false
        }
        firebaseService.addRecipe(recipe,
            onSuccess = { id ->
                recipeIdMap[recipe.title] = id
            },
            onError = {}
        )
        recipes.add(recipe)
        return true
    }

    fun deleteRecipe(context: android.content.Context, title: String): Boolean {
        val key = recipeIdMap[title]
        if (key != null) {
            firebaseService.deleteRecipe(key, onSuccess = {}, onError = {})
        }
        val removed = recipes.removeIf { it.title.equals(title, ignoreCase = true) }
        return removed
    }

    fun getAllRecipes(): List<Recipe> {
        return recipes
    }

    fun updateRecipe(context: android.content.Context, oldTitle: String, updatedRecipe: Recipe): Boolean {
        val index = recipes.indexOfFirst {
            it.title.equals(oldTitle, ignoreCase = true)
        }

        return if (index != -1) {
            recipes[index] = updatedRecipe
            val key = recipeIdMap[oldTitle]
            if (key != null) {
                recipeIdMap.remove(oldTitle)
                recipeIdMap[updatedRecipe.title] = key
                firebaseService.updateRecipe(key, updatedRecipe, onSuccess = {}, onError = {})
            }
            true
        } else {
            false
        }
    }



}
