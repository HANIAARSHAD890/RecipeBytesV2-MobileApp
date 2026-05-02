package com.example.recipebytes.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.adapters.IngredientAdapter
import com.example.recipebytes.R
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.Step
import com.example.recipebytes.adapters.StepsAdapter
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Screen for viewing and editing the details of a specific recipe.
 */
class RecipeViewDetailsScreen : AppCompatActivity() {
    private var isCurrentlyEditing = false
    private lateinit var ingAdapter: IngredientAdapter
    private lateinit var stepAdapter: StepsAdapter
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilTitle: TextInputLayout
    private lateinit var tilDesc: TextInputLayout
    private lateinit var tilCategory: TextInputLayout
    private lateinit var etDesc: TextInputEditText
    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var primaryIcon: ImageView
    private lateinit var recipeImage: ImageView
    private lateinit var ingredientsRecycler: RecyclerView
    private lateinit var stepsRecycler: RecyclerView
    private lateinit var btnUpdate: Button
    private var recipe: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_view_details_screen)
        
        initializeViews()
        setupCategoryAdapter()
        handleIntentData()
        setupClickListeners()
        setupDynamicErrorClearing()
    }

    /**
     * Finds and assigns all UI components from the layout.
     */
    private fun initializeViews() {
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        headerTitle.text = "View Recipe"
        
        btnUpdate = findViewById(R.id.btnUpdateRecipe)
        recipeImage = findViewById(R.id.recipeImage)
        etTitle = findViewById(R.id.etRecipeTitle)
        etDesc = findViewById(R.id.etRecipeDesc)
        etCategory = findViewById(R.id.etRecipeCategory)
        primaryIcon = findViewById(R.id.primaryIcon)
        tilTitle = findViewById(R.id.tilRecipeTitle)
        tilDesc = findViewById(R.id.tilRecipeDesc)
        tilCategory = findViewById(R.id.tilRecipeCategory)
        ingredientsRecycler = findViewById(R.id.ingredientsRecycler)
        stepsRecycler = findViewById(R.id.stepsRecycler)
    }

    /**
     * Sets up the category dropdown adapter and initial state.
     */
    private fun setupCategoryAdapter() {
        val categories = arrayOf("Breakfast", "Lunch", "Dinner", "Dessert")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(adapter)
        etCategory.isEnabled = false
        tilCategory.isEnabled = false
    }

    /**
     * Retrieves recipe data from the intent and populates the UI.
     */
    private fun handleIntentData() {
        recipe = intent.getSerializableExtra("recipe") as? Recipe
        recipe?.let {
            setupInitialData(it)
        } ?: run {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Configures click listeners for the edit icon and update button.
     */
    private fun setupClickListeners() {
        primaryIcon.setImageResource(android.R.drawable.ic_menu_edit)
        primaryIcon.visibility = View.VISIBLE
        primaryIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.icon_disabled)

        primaryIcon.setOnClickListener {
            if (!isCurrentlyEditing) {
                enterEditMode()
                btnUpdate.visibility = View.VISIBLE
            }
        }

        btnUpdate.setOnClickListener {
            syncDataFromUI()
            if (validateAllFields()) {
                saveAndExitEditMode()
                btnUpdate.visibility = View.GONE
                primaryIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.buttontext)
                Toast.makeText(this, "Recipe Updated Successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Populates the fields and recyclerviews with the provided recipe data.
     */
    private fun setupInitialData(recipe: Recipe) {
        etCategory.isEnabled = false
        tilCategory.isEnabled = false
        etCategory.setOnClickListener(null)
        etTitle.setText(recipe.title)
        etDesc.setText(recipe.description)
        etCategory.setText(recipe.category, false)
        
        recipe.imageUri?.let {
            Glide.with(this).load(it).centerCrop().into(recipeImage)
        }
        
        ingAdapter = IngredientAdapter(recipe.ingredients.toMutableList(), false)
        ingredientsRecycler.layoutManager = LinearLayoutManager(this)
        ingredientsRecycler.adapter = ingAdapter
        
        stepAdapter = StepsAdapter(recipe.steps.toMutableList(), false)
        stepsRecycler.layoutManager = LinearLayoutManager(this)
        stepsRecycler.adapter = stepAdapter
    }

    /**
     * Validates that all required fields and list items are correctly filled.
     */
    private fun validateAllFields(): Boolean {
        var isValid = true

        if (etDesc.text.toString().trim().isEmpty()) {
            tilDesc.error = "Description cannot be blank"
            isValid = false
        }
        if (etCategory.text.toString().trim().isEmpty()) {
            tilCategory.error = "Category cannot be blank"
            isValid = false
        }

        recipe?.ingredients?.forEachIndexed { index, ingredient ->
            if (ingredient.name.trim().isEmpty() || ingredient.quantity.trim().isEmpty()) {
                isValid = false
                val holder = ingredientsRecycler.findViewHolderForAdapterPosition(index)
                if (holder is IngredientAdapter.ViewHolder) {
                    holder.tilName?.error = "Required"
                    holder.tilQty?.error = "Required"
                }
            }
        }

        recipe?.steps?.forEachIndexed { index, step ->
            if (step.stepcontent.trim().isEmpty()) {
                isValid = false
                val holder = stepsRecycler.findViewHolderForAdapterPosition(index)
                if (holder is StepsAdapter.ViewHolder) {
                    holder.tilStepContent?.error = "Step content required"
                }
            }
        }

        if (!isValid) {
            Toast.makeText(this, "Please correct the errors before updating", Toast.LENGTH_SHORT).show()
        }
        return isValid
    }

    /**
     * Adds text change listeners to clear error messages automatically.
     */
    private fun setupDynamicErrorClearing() {
        etDesc.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) {
                tilDesc.error = null
            }
        }
        etCategory.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) {
                tilCategory.error = null
            }
        }
    }

    /**
     * Enables editing mode for all recipe fields and lists.
     */
    private fun enterEditMode() {
        isCurrentlyEditing = true
        btnUpdate.visibility = View.VISIBLE
        primaryIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.buttontext)
        primaryIcon.setImageResource(android.R.drawable.ic_menu_save)

        etTitle.isEnabled = false
        etDesc.isEnabled = true
        etCategory.isEnabled = true
        tilCategory.isEnabled = true
        etDesc.requestFocus()

        ingAdapter.isEditMode = true
        stepAdapter.isEditable = true
        ingAdapter.notifyDataSetChanged()
        stepAdapter.notifyDataSetChanged()
    }

    /**
     * Updates the recipe object with data from the input fields.
     */
    private fun syncDataFromUI() {
        recipe?.let {
            it.description = etDesc.text.toString().trim()
            it.category = etCategory.text.toString().trim()
        }
    }

    /**
     * Saves the modified recipe to the repository and disables editing mode.
     */
    private fun saveAndExitEditMode() {
        isCurrentlyEditing = false
        recipe?.let {
            RecipeRepository.updateRecipe(this, it.title, it)
        }

        btnUpdate.visibility = View.GONE
        primaryIcon.setImageResource(android.R.drawable.ic_menu_edit)
        primaryIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.icon_disabled)

        etTitle.isEnabled = false
        etDesc.isEnabled = false
        etCategory.isEnabled = false
        tilCategory.isEnabled = false
        etCategory.setOnClickListener(null)
        ingAdapter.isEditMode = false
        stepAdapter.isEditable = false
        ingAdapter.notifyDataSetChanged()
        stepAdapter.notifyDataSetChanged()
    }
}
