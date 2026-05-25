package com.example.recipebytes.models

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import java.io.Serializable

/**
 * Data class representing a recipe with its details, ingredients, and steps.

 */
data class Recipe(
    var title: String,
    var description: String,
    var category: String,
    val imageUri: String? = null,
    val ingredients: List<Ingredient>,
    val steps: List<Step>,
    var cookingTime: String = "",
    var isFavorite: Boolean = false
) : Serializable

/**
 * Repository object for managing recipe data, including persistence and CRUD operations.
 */
object RecipeRepository {

    private var recipes = mutableListOf(
        Recipe(
            title = "Pancake",
            description = "Soft and fluffy breakfast pancakes.",
            category = "Breakfast",
            imageUri = "https://www.inspiredtaste.net/wp-content/uploads/2025/07/Pancake-Recipe-1.jpg",
            ingredients = listOf(
                Ingredient("Flour", "100"),
                Ingredient("Milk", "150"),
                Ingredient("Egg", "50"),
                Ingredient("Sugar", "20"),
                Ingredient("Baking Powder", "5"),
                Ingredient("Butter", "20")
            ),
            steps = listOf(
                Step("Mix all ingredients"),
                Step("Pour batter on pan"),
                Step("Cook both sides till golden")
            )
        ),
        Recipe(
            title = "Anda Paratha",
            description = "Egg stuffed crispy,golden paratha.",
            category = "Breakfast",
            imageUri = "https://hinzcooking.com/wp-content/uploads/2018/05/egg-paratha.jpg",
            ingredients = listOf(
                Ingredient("Flour", "100"),
                Ingredient("Egg", "50"),
                Ingredient("Oil", "10"),
                Ingredient("Salt", "2")
            ),
            steps = listOf(
                Step("Make dough"),
                Step("Add egg filling"),
                Step("Cook on pan")
            )
        ),
        Recipe(
            title = "Chicken Biryani",
            description = "Spicy Pakistani rice dish.",
            category = "Lunch",
            imageUri = "https://i.ytimg.com/vi/usc-TQ-17lI/sddefault.jpg",
            ingredients = listOf(
                Ingredient("Rice", "500"),
                Ingredient("Chicken", "500"),
                Ingredient("Yogurt", "200"),
                Ingredient("Biryani Masala", "30"),
                Ingredient("Onions", "300"),
                Ingredient("Tomatoes", "200"),
                Ingredient("Ginger Garlic Paste", "40"),
                Ingredient("Red Chili Powder", "5"),
                Ingredient("Turmeric", "3"),
                Ingredient("Oil or Ghee", "60"),
                Ingredient("Fresh Mint", "20"),
                Ingredient("Fresh Coriander", "20"),
                Ingredient("Saffron", "1"),
                Ingredient("Salt", "12"),
                Ingredient("Water", "1500")
            ),
            steps = listOf(
                Step("Soak basmati rice in water for 30 minutes then drain"),
                Step("Fry sliced onions in oil until golden brown, remove half for garnish"),
                Step("Add whole spices and ginger garlic paste, sauté for 1 minute"),
                Step("Add tomatoes and cook until oil separates"),
                Step("Add chicken with yogurt, biryani masala, chili powder, turmeric and salt, cook until tender"),
                Step("Boil water with salt, add drained rice and parboil until 70% done then drain"),
                Step("Layer chicken masala at bottom, top with parboiled rice"),
                Step("Garnish with fried onions, mint, coriander and saffron milk"),
                Step("Seal pot and steam on low heat for 20 minutes"),
                Step("Rest for 10 minutes, mix gently and serve hot")
            )
        ),
        Recipe(
            title = "Spaghetti Marinara",
            description = "Simple tomato pasta.",
            category = "Lunch",
            imageUri = "https://realfood.tesco.com/media/images/RFO-SpaghettiAl-LargeHero-1400x919-mini-83f49843-c092-4830-b2f6-a3fa90d1328c-0-1400x920.jpg",
            ingredients = listOf(
                Ingredient("Spaghetti", "120"),
                Ingredient("Tomato Sauce", "150"),
                Ingredient("Garlic", "5")
            ),
            steps = listOf(
                Step("Boil pasta"),
                Step("Cook sauce"),
                Step("Mix and serve")
            )
        ),
        Recipe(
            title = "Chicken Karahi",
            description = "Traditional chicken curry.",
            category = "Dinner",
            imageUri = "https://cookingwithnelo.com/wp-content/uploads/2025/06/restaurant-style-chicken-karahi.jpg",
            ingredients = listOf(
                Ingredient("Chicken", "250"),
                Ingredient("Tomato", "150"),
                Ingredient("Oil", "30")
            ),
            steps = listOf(
                Step("Cook chicken"),
                Step("Add tomatoes"),
                Step("Cook till thick")
            )
        ),
        Recipe(
            title = "White Sauce Pasta",
            description = "Creamy pasta dish.",
            category = "Dinner",
            imageUri = "https://www.indianhealthyrecipes.com/wp-content/uploads/2024/04/white-sauce-pasta-500x500.jpg",
            ingredients = listOf(
                Ingredient("Pasta", "120"),
                Ingredient("Milk", "200"),
                Ingredient("Butter", "20"),
                Ingredient("Cheese", "50")
            ),
            steps = listOf(
                Step("Make white sauce"),
                Step("Add pasta"),
                Step("Mix cheese")
            )
        ),
        Recipe(
            title = "Kheer",
            description = "Sweet rice pudding.",
            category = "Dessert",
            imageUri = "https://www.indianveggiedelight.com/wp-content/uploads/2017/08/rice-kheer-instant-pot-featured-image-500x500.jpg",
            ingredients = listOf(
                Ingredient("Rice", "50"),
                Ingredient("Milk", "500"),
                Ingredient("Sugar", "80")
            ),
            steps = listOf(
                Step("Cook rice in milk"),
                Step("Add sugar"),
                Step("Simmer till thick")
            )
        ),
        Recipe(
            title = "Tiramisu",
            description = "Coffee layered dessert.",
            category = "Dessert",
            imageUri = "https://www.micheldumas.com/wp-content/uploads/2025/05/tiramisu1-ezgif.com-jpg-to-webp-converter-1-1.webp",
            ingredients = listOf(
                Ingredient("Biscuits", "100"),
                Ingredient("Coffee", "100"),
                Ingredient("Cream", "200")
            ),
            steps = listOf(
                Step("Dip biscuits"),
                Step("Layer with cream"),
                Step("Chill and serve")
            )
        ),
        Recipe(
            title = "Banana Bread",
            description = "Soft baked bread.",
            category = "Baked Goods",
            imageUri = "https://www.allrecipes.com/thmb/fAkQn-FhjF89oTJ5JXpgwvwNf34=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/20144-banana-banana-bread-VAT-009-4x3-B-78f1cfc64bfa451e8a0fead814719b9f.jpg",
            ingredients = listOf(
                Ingredient("Flour", "150"),
                Ingredient("Banana", "120"),
                Ingredient("Sugar", "80")
            ),
            steps = listOf(
                Step("Mix ingredients"),
                Step("Pour in mold"),
                Step("Bake at 180C")
            )
        ),
        Recipe(
            title = "Chicken Patties",
            description = "Bakery style snack.",
            category = "Baked Goods",
            imageUri = "https://i.ytimg.com/vi/XOQWz4ANzyg/sddefault.jpg",
            ingredients = listOf(
                Ingredient("Puff pastry", "200"),
                Ingredient("Chicken", "150"),
                Ingredient("Onion", "50")
            ),
            steps = listOf(
                Step("Prepare filling"),
                Step("Fill pastry"),
                Step("Bake till golden")
            )
        )
    )

    private const val PREFS_NAME = "recipe_prefs"
    private const val KEY_RECIPES = "recipes_list"

    /**
     * Initializes the repository by loading saved recipes from SharedPreferences.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECIPES, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Recipe>>() {}.type
            recipes = Gson().fromJson(json, type)
        }
    }

    /**
     * Persists the current recipe list to SharedPreferences.
     */
    private fun saveToDisk(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(recipes)
        prefs.edit().putString(KEY_RECIPES, json).apply()
    }

    /**
     * Adds a new recipe to the repository. Returns false if a recipe with the same title already exists.
     */
    fun addRecipe(context: Context, recipe: Recipe): Boolean {
        if (recipes.any { it.title == recipe.title }) {
            return false
        }
        recipes.add(recipe)
        saveToDisk(context)
        return true
    }

    /**
     * Deletes a recipe from the repository by its title.
     */
    fun deleteRecipe(context: Context, title: String): Boolean {
        val removed = recipes.removeIf { it.title.equals(title, ignoreCase = true) }
        if (removed) {
            saveToDisk(context)
        }
        return removed
    }

    /**
     * Returns a list of all available recipes.
     */
    fun getAllRecipes(): List<Recipe> {
        return recipes
    }

    /**
     * Updates an existing recipe by its original title.
     */
    fun updateRecipe(context: Context, oldTitle: String, updatedRecipe: Recipe): Boolean {
        val index = recipes.indexOfFirst {
            it.title.equals(oldTitle, ignoreCase = true)
        }

        return if (index != -1) {
            recipes[index] = updatedRecipe
            saveToDisk(context)
            true
        } else {
            false
        }
    }
    fun toggleFavorite(context: Context, title: String): Boolean {
        val recipe = recipes.find { it.title.equals(title, ignoreCase = true) }
        recipe?.let {
            it.isFavorite = !it.isFavorite
            saveToDisk(context)
            return it.isFavorite
        }
        return false
    }

    fun getFavorites(): List<Recipe> {
        return recipes.filter { it.isFavorite }
    }

}
