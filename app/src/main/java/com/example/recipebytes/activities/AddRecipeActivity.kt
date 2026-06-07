package com.example.recipebytes.activities

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.recipebytes.fragments.AddRecipeFragment1
import com.example.recipebytes.fragments.AddRecipeFragment2
import com.example.recipebytes.fragments.AddRecipeFragment3
import com.example.recipebytes.fragments.AddRecipeFragment4
import com.example.recipebytes.fragments.AddRecipeFragmentNutrition
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Nutrition
import com.example.recipebytes.R
import com.example.recipebytes.models.Step
import com.example.recipebytes.services.DraftService

// Multi-step activity for creating a new recipe with fragments
class AddRecipeActivity : AppCompatActivity() {

    private var title       = ""
    private var desc        = ""
    private var category    = ""
    private var cookingTime = ""
    private var ingredients = ArrayList<Ingredient>()
    private var stepsList   = ArrayList<Step>()
    private var nutrition   = Nutrition()

    // Getters for fragments to access activity-level data for draft saving
    fun getRecipeTitle()       = title
    fun getRecipeDesc()        = desc
    fun getRecipeCategory()    = category
    fun getRecipeCookingTime() = cookingTime
    fun getRecipeIngredients() = ingredients

    // Initializes the activity and launches the appropriate first fragment based on mode
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_recipe)
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        headerTitle.text = "Add Recipe"
        if (savedInstanceState == null) {
            val mode = intent.getStringExtra("mode")

            when (mode) {
                "prefilled" -> {
                    title       = intent.getStringExtra("title")       ?: ""
                    desc        = intent.getStringExtra("desc")        ?: ""
                    category    = intent.getStringExtra("category")    ?: ""
                    cookingTime = intent.getStringExtra("cookingTime") ?: ""

                    @Suppress("UNCHECKED_CAST", "DEPRECATION")
                    ingredients = intent.getSerializableExtra("ingredients") as? ArrayList<Ingredient> ?: ArrayList()

                    @Suppress("UNCHECKED_CAST", "DEPRECATION")
                    stepsList = intent.getSerializableExtra("steps") as? ArrayList<Step> ?: ArrayList()

                    val bundle = Bundle().apply {
                        putString("prefillTitle",       title)
                        putString("prefillDesc",        desc)
                        putString("prefillCategory",    category)
                        putString("prefillCookingTime", cookingTime)
                        // Pass ingredients and steps directly in bundle too
                        putSerializable("prefillIngredients", ingredients)
                        putSerializable("prefillSteps",       stepsList)
                    }
                    val fragment = AddRecipeFragment1()
                    fragment.arguments = bundle
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit()
                }
                "ai" -> {
                    // AI mode — go to Step 1 with AI section focused
                    val bundle = Bundle().apply { putBoolean("focusAI", true) }
                    val fragment = AddRecipeFragment1()
                    fragment.arguments = bundle
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit()
                }
                else -> {
                    // Manual — normal flow
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AddRecipeFragment1())
                        .commit()
                }
            }
        }
    }

    // STEP 1 → STEP 2
    // Navigates from step 1 (details) to step 2 (ingredients)
    fun goToStep2(titler: String, descr: String, categoryr: String, cookingTimer: String) {
        title       = titler
        desc        = descr
        category    = categoryr
        cookingTime = cookingTimer
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AddRecipeFragment2())
            .addToBackStack(null)
            .commit()
    }

    // STEP 1 → STEP 4 (AI flow)
    // Navigates from step 1 directly to step 4 with AI-generated ingredients and steps
    fun goToStep2WithAI(
        titler: String, descr: String, categoryr: String, cookingTimer: String,
        ingredientsList: List<Ingredient>, stepsList: List<Step>
    ) {
        title       = titler
        desc        = descr
        category    = categoryr
        cookingTime = cookingTimer
        ingredients = ArrayList(ingredientsList)

        val bundle = Bundle().apply {
            putString("title",       title)
            putString("desc",        desc)
            putString("category",    category)
            putString("cookingTime", cookingTime)
            putSerializable("ingredients", ingredients)
            putSerializable("steps",       ArrayList(stepsList))
        }
        val fragment = AddRecipeFragment4()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // STEP 2 → NUTRITION
    // Navigates from step 2 (ingredients) to the nutrition step
    fun goToStep3(ingredientsList: List<Ingredient>) {
        ingredients = ArrayList(ingredientsList)
        val bundle = Bundle().apply {
            putSerializable("ingredients", ingredients)
        }
        val fragment = AddRecipeFragmentNutrition()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // NUTRITION → STEP 3 (Steps)
    // Navigates from the nutrition step to step 3 (cooking steps)
    fun goToStep3WithNutrition(nutritionData: Nutrition) {
        nutrition = nutritionData
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AddRecipeFragment3())
            .addToBackStack(null)
            .commit()
    }

    // STEP 3 → STEP 4
    // Navigates from step 3 to the final review step with all recipe data
    fun goToStep4(steps: List<Step>) {
        stepsList = ArrayList(steps)
        val bundle = Bundle().apply {
            putString("title",       title)
            putString("desc",        desc)
            putString("category",    category)
            putString("cookingTime", cookingTime)
            putSerializable("ingredients", ingredients)
            putSerializable("steps",       ArrayList(steps))
            putSerializable("nutrition",   nutrition)
        }
        val fragment = AddRecipeFragment4()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // STEP 1 → STEP 2 (AI generated but editable — goes through full flow)
    // Navigates from step 1 to step 2 with AI-generated data that is still editable
    fun goToStep2WithAIEditable(
        titler: String, descr: String, categoryr: String, cookingTimer: String,
        ingredientsList: List<Ingredient>, aiStepsList: List<Step>
    ) {
        title       = titler
        desc        = descr
        category    = categoryr
        cookingTime = cookingTimer
        ingredients = ArrayList(ingredientsList)
        stepsList   = ArrayList(aiStepsList)

        val bundle = Bundle().apply {
            putSerializable("aiIngredients", ingredients)
        }
        val fragment = AddRecipeFragment2()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Getter so Fragment3 can access AI-generated steps
    fun getAiSteps() = stepsList
    // DRAFT → Skip to nutrition (draft had ingredients)
    // Skips to nutrition step from draft when ingredients already exist
    fun goToStep2FromDraft(
        titler: String, descr: String, categoryr: String,
        cookingTimer: String, ingredientsList: List<Ingredient>
    ) {
        title       = titler
        desc        = descr
        category    = categoryr
        cookingTime = cookingTimer
        ingredients = ArrayList(ingredientsList)
        val bundle = Bundle().apply {
            putSerializable("ingredients", ingredients)
        }
        val fragment = AddRecipeFragmentNutrition()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // DRAFT → Skip to steps (draft had ingredients + nutrition)
    // Skips to steps step from draft when ingredients and nutrition already exist
    fun goToStep3FromDraft(
        titler: String, descr: String, categoryr: String,
        cookingTimer: String, ingredientsList: List<Ingredient>,
        nutritionData: Nutrition?
    ) {
        title       = titler
        desc        = descr
        category    = categoryr
        cookingTime = cookingTimer
        ingredients = ArrayList(ingredientsList)
        nutrition   = nutritionData ?: Nutrition()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AddRecipeFragment3())
            .addToBackStack(null)
            .commit()
    }

    // Called from AddRecipeFragment4 when recipe is saved — clears draft
    // Clears the draft after the recipe is successfully saved
    fun onRecipeSaved() {
        DraftService.clearDraft()
    }
}