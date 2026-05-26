package com.example.recipebytes.services

import android.content.Context
import android.widget.Toast
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Nutrition
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.Step

object RecipeSeeder {

    private val firebaseService = FirebaseRecipeService()

    private val seedRecipes = listOf(
        Recipe(
            title = "Pancake",
            description = "Soft and fluffy breakfast pancakes.",
            category = "Breakfast",
            imageUri = "https://www.inspiredtaste.net/wp-content/uploads/2025/07/Pancake-Recipe-1.jpg",
            cookingTime = 15,
            servings = 2,
            ingredients = listOf(
                Ingredient("Flour", "100g"),
                Ingredient("Milk", "150ml"),
                Ingredient("Egg", "1"),
                Ingredient("Sugar", "20g"),
                Ingredient("Baking Powder", "5g"),
                Ingredient("Butter", "20g")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Mix all ingredients"),
                Step(stepId = 2, text = "Pour batter on pan"),
                Step(stepId = 3, text = "Cook both sides till golden")
            ),
            nutrition = Nutrition(calories = 350, protein = "8g")
        ),
        Recipe(
            title = "Anda Paratha",
            description = "Egg stuffed crispy,golden paratha.",
            category = "Breakfast",
            imageUri = "https://hinzcooking.com/wp-content/uploads/2018/05/egg-paratha.jpg",
            cookingTime = 20,
            servings = 2,
            ingredients = listOf(
                Ingredient("Flour", "100g"),
                Ingredient("Egg", "1"),
                Ingredient("Oil", "10ml"),
                Ingredient("Salt", "2g")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Make dough"),
                Step(stepId = 2, text = "Add egg filling"),
                Step(stepId = 3, text = "Cook on pan")
            ),
            nutrition = Nutrition(calories = 300, protein = "10g")
        ),
        Recipe(
            title = "Chicken Biryani",
            description = "Spicy Pakistani rice dish.",
            category = "Lunch",
            imageUri = "https://i.ytimg.com/vi/usc-TQ-17lI/sddefault.jpg",
            cookingTime = 60,
            servings = 4,
            ingredients = listOf(
                Ingredient("Rice", "500g"),
                Ingredient("Chicken", "500g"),
                Ingredient("Yogurt", "200ml"),
                Ingredient("Biryani Masala", "30g"),
                Ingredient("Onions", "300g"),
                Ingredient("Tomatoes", "200g"),
                Ingredient("Ginger Garlic Paste", "40g"),
                Ingredient("Red Chili Powder", "5g"),
                Ingredient("Turmeric", "3g"),
                Ingredient("Oil or Ghee", "60ml"),
                Ingredient("Fresh Mint", "20g"),
                Ingredient("Fresh Coriander", "20g"),
                Ingredient("Saffron", "1g"),
                Ingredient("Salt", "12g"),
                Ingredient("Water", "1500ml")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Soak basmati rice in water for 30 minutes then drain"),
                Step(stepId = 2, text = "Fry sliced onions in oil until golden brown, remove half for garnish"),
                Step(stepId = 3, text = "Add whole spices and ginger garlic paste, sauté for 1 minute"),
                Step(stepId = 4, text = "Add tomatoes and cook until oil separates"),
                Step(stepId = 5, text = "Add chicken with yogurt, biryani masala, chili powder, turmeric and salt, cook until tender"),
                Step(stepId = 6, text = "Boil water with salt, add drained rice and parboil until 70% done then drain"),
                Step(stepId = 7, text = "Layer chicken masala at bottom, top with parboiled rice"),
                Step(stepId = 8, text = "Garnish with fried onions, mint, coriander and saffron milk"),
                Step(stepId = 9, text = "Seal pot and steam on low heat for 20 minutes"),
                Step(stepId = 10, text = "Rest for 10 minutes, mix gently and serve hot")
            ),
            nutrition = Nutrition(calories = 650, protein = "35g")
        ),
        Recipe(
            title = "Spaghetti Marinara",
            description = "Simple tomato pasta.",
            category = "Lunch",
            imageUri = "https://realfood.tesco.com/media/images/RFO-SpaghettiAl-LargeHero-1400x919-mini-83f49843-c092-4830-b2f6-a3fa90d1328c-0-1400x920.jpg",
            cookingTime = 20,
            servings = 2,
            ingredients = listOf(
                Ingredient("Spaghetti", "120g"),
                Ingredient("Tomato Sauce", "150ml"),
                Ingredient("Garlic", "5g")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Boil pasta"),
                Step(stepId = 2, text = "Cook sauce"),
                Step(stepId = 3, text = "Mix and serve")
            ),
            nutrition = Nutrition(calories = 400, protein = "12g")
        ),
        Recipe(
            title = "Chicken Karahi",
            description = "Traditional chicken curry.",
            category = "Dinner",
            imageUri = "https://cookingwithnelo.com/wp-content/uploads/2025/06/restaurant-style-chicken-karahi.jpg",
            cookingTime = 35,
            servings = 3,
            ingredients = listOf(
                Ingredient("Chicken", "250g"),
                Ingredient("Tomato", "150g"),
                Ingredient("Oil", "30ml")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Cook chicken"),
                Step(stepId = 2, text = "Add tomatoes"),
                Step(stepId = 3, text = "Cook till thick")
            ),
            nutrition = Nutrition(calories = 450, protein = "28g")
        ),
        Recipe(
            title = "White Sauce Pasta",
            description = "Creamy pasta dish.",
            category = "Dinner",
            imageUri = "https://www.indianhealthyrecipes.com/wp-content/uploads/2024/04/white-sauce-pasta-500x500.jpg",
            cookingTime = 25,
            servings = 2,
            ingredients = listOf(
                Ingredient("Pasta", "120g"),
                Ingredient("Milk", "200ml"),
                Ingredient("Butter", "20g"),
                Ingredient("Cheese", "50g")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Make white sauce"),
                Step(stepId = 2, text = "Add pasta"),
                Step(stepId = 3, text = "Mix cheese")
            ),
            nutrition = Nutrition(calories = 500, protein = "15g")
        ),
        Recipe(
            title = "Kheer",
            description = "Sweet rice pudding.",
            category = "Dessert",
            imageUri = "https://www.indianveggiedelight.com/wp-content/uploads/2017/08/rice-kheer-instant-pot-featured-image-500x500.jpg",
            cookingTime = 30,
            servings = 4,
            ingredients = listOf(
                Ingredient("Rice", "50g"),
                Ingredient("Milk", "500ml"),
                Ingredient("Sugar", "80g")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Cook rice in milk"),
                Step(stepId = 2, text = "Add sugar"),
                Step(stepId = 3, text = "Simmer till thick")
            ),
            nutrition = Nutrition(calories = 300, protein = "6g")
        ),
        Recipe(
            title = "Tiramisu",
            description = "Coffee layered dessert.",
            category = "Dessert",
            imageUri = "https://www.micheldumas.com/wp-content/uploads/2025/05/tiramisu1-ezgif.com-jpg-to-webp-converter-1-1.webp",
            cookingTime = 20,
            servings = 4,
            ingredients = listOf(
                Ingredient("Biscuits", "100g"),
                Ingredient("Coffee", "100ml"),
                Ingredient("Cream", "200ml")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Dip biscuits"),
                Step(stepId = 2, text = "Layer with cream"),
                Step(stepId = 3, text = "Chill and serve")
            ),
            nutrition = Nutrition(calories = 380, protein = "5g")
        ),
        Recipe(
            title = "Banana Bread",
            description = "Soft baked bread.",
            category = "Baked Goods",
            imageUri = "https://www.allrecipes.com/thmb/fAkQn-FhjF89oTJ5JXpgwvwNf34=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/20144-banana-banana-bread-VAT-009-4x3-B-78f1cfc64bfa451e8a0fead814719b9f.jpg",
            cookingTime = 50,
            servings = 6,
            ingredients = listOf(
                Ingredient("Flour", "150g"),
                Ingredient("Banana", "120g"),
                Ingredient("Sugar", "80g")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Mix ingredients"),
                Step(stepId = 2, text = "Pour in mold"),
                Step(stepId = 3, text = "Bake at 180C")
            ),
            nutrition = Nutrition(calories = 250, protein = "4g")
        ),
        Recipe(
            title = "Chicken Patties",
            description = "Bakery style snack.",
            category = "Baked Goods",
            imageUri = "https://i.ytimg.com/vi/XOQWz4ANzyg/sddefault.jpg",
            cookingTime = 30,
            servings = 4,
            ingredients = listOf(
                Ingredient("Puff pastry", "200g"),
                Ingredient("Chicken", "150g"),
                Ingredient("Onion", "50g")
            ),
            steps = listOf(
                Step(stepId = 1, text = "Prepare filling"),
                Step(stepId = 2, text = "Fill pastry"),
                Step(stepId = 3, text = "Bake till golden")
            ),
            nutrition = Nutrition(calories = 320, protein = "18g")
        )
    )

    fun seedAll(context: Context, userId: String = "", onComplete: () -> Unit = {}) {
        var remaining = seedRecipes.size
        var successCount = 0
        var failCount = 0

        for (recipe in seedRecipes) {
            val recipeWithOwner = if (userId.isNotEmpty()) {
                recipe.copy(userId = userId)
            } else {
                recipe
            }
            firebaseService.addRecipe(
                recipe = recipeWithOwner,
                onSuccess = {
                    successCount++
                    remaining--
                    if (remaining == 0) {
                        val msg = "Seeded $successCount recipes (${failCount} failed)"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onComplete()
                    }
                },
                onError = { error ->
                    failCount++
                    remaining--
                    android.util.Log.e("RecipeSeeder", "Failed to seed ${recipe.title}: $error")
                    if (remaining == 0) {
                        val msg = "Seeded $successCount recipes (${failCount} failed)"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onComplete()
                    }
                }
            )
        }
    }
}
