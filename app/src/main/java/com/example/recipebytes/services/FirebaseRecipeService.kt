package com.example.recipebytes.services

import android.util.Log
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Nutrition
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.Step
import com.example.recipebytes.models.User
import com.google.firebase.database.*

class FirebaseRecipeService {

    private val database = FirebaseDatabase.getInstance().reference
    private val recipesRef = database.child("recipes")

    companion object {
        private const val TAG = "FirebaseRecipeService"
    }

    fun getAllRecipes(
        onSuccess: (recipes: List<Pair<String, Recipe>>) -> Unit,
        onError: (error: String) -> Unit
    ) {
        recipesRef.get()
            .addOnSuccessListener { snapshot ->
                val recipes = mutableListOf<Pair<String, Recipe>>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val recipe = childToRecipe(child)
                    if (recipe != null) {
                        recipes.add(Pair(key, recipe))
                    }
                }
                Log.d(TAG, "Fetched ${recipes.size} recipes")
                onSuccess(recipes)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error fetching recipes: ${error.message}")
                onError(error.message ?: "Failed to fetch recipes")
            }
    }

    fun addRecipe(
        recipe: Recipe,
        onSuccess: (recipeId: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val newRef = recipesRef.push()
        newRef.setValue(recipeToMap(recipe))
            .addOnSuccessListener {
                val id = newRef.key ?: ""
                Log.d(TAG, "Recipe added with ID: $id")
                onSuccess(id)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error adding recipe: ${error.message}")
                onError(error.message ?: "Failed to add recipe")
            }
    }

    fun updateRecipe(
        recipeId: String,
        recipe: Recipe,
        onSuccess: () -> Unit,
        onError: (error: String) -> Unit
    ) {
        recipesRef.child(recipeId).setValue(recipeToMap(recipe))
            .addOnSuccessListener {
                Log.d(TAG, "Recipe updated: $recipeId")
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error updating recipe: ${error.message}")
                onError(error.message ?: "Failed to update recipe")
            }
    }

    fun deleteRecipe(
        recipeId: String,
        onSuccess: () -> Unit,
        onError: (error: String) -> Unit
    ) {
        recipesRef.child(recipeId).removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "Recipe deleted: $recipeId")
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error deleting recipe: ${error.message}")
                onError(error.message ?: "Failed to delete recipe")
            }
    }

    fun addFavorite(userId: String, recipeId: String) {
        database.child("users").child(userId).child("favorites").child(recipeId).setValue(true)
            .addOnSuccessListener { Log.d(TAG, "Favorite added: $recipeId") }
            .addOnFailureListener { e -> Log.e(TAG, "Error adding favorite: ${e.message}") }
    }

    fun removeFavorite(userId: String, recipeId: String) {
        database.child("users").child(userId).child("favorites").child(recipeId).removeValue()
            .addOnSuccessListener { Log.d(TAG, "Favorite removed: $recipeId") }
            .addOnFailureListener { e -> Log.e(TAG, "Error removing favorite: ${e.message}") }
    }

    fun getFavoriteIds(userId: String, onResult: (Set<String>) -> Unit) {
        database.child("users").child(userId).child("favorites").get()
            .addOnSuccessListener { snapshot ->
                val ids = mutableSetOf<String>()
                for (child in snapshot.children) {
                    child.key?.let { ids.add(it) }
                }
                onResult(ids)
            }
            .addOnFailureListener {
                onResult(emptySet())
            }
    }

    fun addLike(userId: String, recipeId: String) {
        // Add to user's likes set
        database.child("users").child(userId).child("likes").child(recipeId).setValue(true)
        // Add to recipe's likedBy set (for showing who liked it)
        recipesRef.child(recipeId).child("likedBy").child(userId).setValue(true)
        // Update likes count
        recipesRef.child(recipeId).child("likesCount")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = current + 1
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
            })
    }

    fun removeLike(userId: String, recipeId: String) {
        // Remove from user's likes set
        database.child("users").child(userId).child("likes").child(recipeId).removeValue()
        // Remove from recipe's likedBy set
        recipesRef.child(recipeId).child("likedBy").child(userId).removeValue()
        // Update likes count
        recipesRef.child(recipeId).child("likesCount")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Int::class.java) ?: 1
                    currentData.value = (current - 1).coerceAtLeast(0)
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
            })
    }

    fun getLikedIds(userId: String, onResult: (Set<String>) -> Unit) {
        database.child("users").child(userId).child("likes").get()
            .addOnSuccessListener { snapshot ->
                val ids = mutableSetOf<String>()
                for (child in snapshot.children) {
                    child.key?.let { ids.add(it) }
                }
                onResult(ids)
            }
            .addOnFailureListener {
                onResult(emptySet())
            }
    }

    fun getLikedByUsers(recipeId: String, onResult: (Map<String, User>) -> Unit) {
        val likedByRef = database.child("recipes").child(recipeId).child("likedBy")
        likedByRef.get()
            .addOnSuccessListener { snapshot ->
                val likedByMap = hashMapOf<String, User>()
                val childrenCount = snapshot.childrenCount
                if (childrenCount == 0L) {
                    onResult(likedByMap)
                    return@addOnSuccessListener
                }

                for (child in snapshot.children) {
                    val userId = child.key ?: continue
                    // Fetch user details for this userId
                    database.child("users").child(userId).get()
                        .addOnSuccessListener { userSnapshot ->
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                likedByMap[userId] = user
                            }
                            // If we've fetched all users, call the callback
                            if (likedByMap.size.toLong() == childrenCount) {
                                onResult(likedByMap)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error fetching user $userId: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching likedBy for recipe $recipeId: ${e.message}")
                onResult(emptyMap<String, User>())
            }
    }

    private fun childToRecipe(child: DataSnapshot): Recipe? {
        val title = child.child("title").value as? String ?: return null
        val description = child.child("description").value as? String ?: ""
        val category = child.child("category").value as? String ?: ""
        val imageUri = child.child("imageUri").value as? String

        val cookingNum = child.child("cookingTime").value
        val cookingTime = when (cookingNum) {
            is Long -> cookingNum.toInt()
            is Int -> cookingNum
            else -> 0
        }
        val servingsNum = child.child("servings").value
        val servings = when (servingsNum) {
            is Long -> servingsNum.toInt()
            is Int -> servingsNum
            else -> 1
        }
        val isPublic = child.child("isPublic").value as? Boolean ?: true
        val userId = child.child("userId").value as? String ?: ""
        val createdAt = child.child("createdAt").value as? Long ?: System.currentTimeMillis()

        val commentsNum = child.child("commentsCount").value
        val commentsCount = when (commentsNum) {
            is Long -> commentsNum.toInt()
            is Int -> commentsNum
            else -> 0
        }
        val likesNum = child.child("likesCount").value
        val likesCount = when (likesNum) {
            is Long -> likesNum.toInt()
            is Int -> likesNum
            else -> 0
        }

        val ingredients = mutableListOf<Ingredient>()
        for (ingChild in child.child("ingredients").children) {
            val name = ingChild.child("name").value as? String ?: ""
            val quantity = ingChild.child("quantity").value as? String ?: ""
            ingredients.add(Ingredient(name, quantity))
        }
        Log.d(TAG, "Parsed ${ingredients.size} ingredients for recipe: $title")

        val steps = mutableListOf<Step>()
        for (stepChild in child.child("steps").children) {
            val sidRaw = stepChild.child("stepId").value
            val sid = when (sidRaw) {
                is Long -> sidRaw.toInt()
                is Int -> sidRaw
                else -> 0
            }
            val text = stepChild.child("text").value as? String ?: ""
            steps.add(Step(stepId = sid, text = text))
        }
        Log.d(TAG, "Parsed ${steps.size} steps for recipe: $title")

        var nutrition: Nutrition? = null
        val nutritionSnapshot = child.child("nutrition")
        if (nutritionSnapshot.exists()) {
            val calRaw = nutritionSnapshot.child("calories").value
            val calories = when (calRaw) {
                is Long -> calRaw.toInt()
                is Int -> calRaw
                else -> 0
            }
            val protein  = (nutritionSnapshot.child("protein").value as? Number)?.toFloat()  ?: 0f
            val carbs    = (nutritionSnapshot.child("carbs").value as? Number)?.toFloat()    ?: 0f
            val fat      = (nutritionSnapshot.child("fat").value as? Number)?.toFloat()      ?: 0f
            val netCarbs = (nutritionSnapshot.child("netCarbs").value as? Number)?.toFloat() ?: 0f
            nutrition = Nutrition(
                calories = calories,
                protein  = protein,
                carbs    = carbs,
                fat      = fat,
                netCarbs = netCarbs
            )
        }

        return Recipe(
            title = title,
            description = description,
            category = category,
            imageUri = imageUri,
            cookingTime = cookingTime,
            servings = servings,
            isPublic = isPublic,
            userId = userId,
            createdAt = createdAt,
            ingredients = ingredients,
            steps = steps,
            nutrition = nutrition,
            commentsCount = commentsCount,
            likesCount = likesCount
        )
    }

    private fun recipeToMap(recipe: Recipe): Map<String, Any> {
        val ingredientsMap = mutableMapOf<String, Map<String, String>>()
        recipe.ingredients.forEachIndexed { index, ing ->
            ingredientsMap[index.toString()] = mapOf(
                "name" to ing.name,
                "quantity" to ing.quantity
            )
        }

        val stepsMap = mutableMapOf<String, Map<String, Any>>()
        recipe.steps.forEachIndexed { index, step ->
            val sid = if (step.stepId > 0) step.stepId else index + 1
            val key = sid.toString()
            stepsMap[key] = mapOf(
                "stepId" to sid,
                "text" to step.text
            )
        }

        val map = mutableMapOf<String, Any>(
            "title" to recipe.title,
            "description" to recipe.description,
            "category" to recipe.category,
            "imageUri" to (recipe.imageUri ?: ""),
            "cookingTime" to recipe.cookingTime,
            "servings" to recipe.servings,
            "isPublic" to recipe.isPublic,
            "userId" to recipe.userId,
            "createdAt" to recipe.createdAt,
            "commentsCount" to recipe.commentsCount,
            "likesCount" to recipe.likesCount,
            "ingredients" to ingredientsMap,
            "steps" to stepsMap
        )

        recipe.nutrition?.let {
            map["nutrition"] = mapOf(
                "calories" to it.calories,
                "protein"  to it.protein,
                "carbs"    to it.carbs,
                "fat"      to it.fat,
                "netCarbs" to it.netCarbs
            )
        }

        return map
    }

    fun fetchUserDashboardMetrics(
        userId: String,
        onSuccess: (totalRecipes: Int, publicCount: Int, privateCount: Int, totalLikes: Int) -> Unit,
        onError: (error: String) -> Unit
    ) {
        recipesRef.orderByChild("userId").equalTo(userId).get()
            .addOnSuccessListener { snapshot ->
                var totalRecipes = 0
                var publicCount = 0
                var privateCount = 0
                var totalLikes = 0

                for (child in snapshot.children) {
                    val recipe = childToRecipe(child)
                    if (recipe != null) {
                        totalRecipes++
                        if (recipe.isPublic) {
                            publicCount++
                        } else {
                            privateCount++
                        }
                        totalLikes += recipe.likesCount
                    }
                }

                Log.d(TAG, "Dashboard metrics - Total: $totalRecipes, Public: $publicCount, Private: $privateCount, Likes: $totalLikes")
                onSuccess(totalRecipes, publicCount, privateCount, totalLikes)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error fetching dashboard metrics: ${error.message}")
                onError(error.message ?: "Failed to fetch dashboard metrics")
            }
    }
}
