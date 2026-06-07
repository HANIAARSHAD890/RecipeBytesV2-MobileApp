package com.example.recipebytes.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.adapters.IngredientAdapter
import com.example.recipebytes.R
import com.example.recipebytes.models.Nutrition
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.Step
import com.example.recipebytes.adapters.StepsAdapter
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.io.FileOutputStream

class RecipeViewDetailsScreen : AppCompatActivity() {
    private var isCurrentlyEditing = false
    private lateinit var ingAdapter: IngredientAdapter
    private lateinit var stepAdapter: StepsAdapter
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilTitle: TextInputLayout
    private lateinit var tilDesc: TextInputLayout
    private lateinit var tilCategory: TextInputLayout
    private lateinit var tilCookingTime: TextInputLayout          // NEW
    private lateinit var etDesc: TextInputEditText
    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var etCookingTime: TextInputEditText         // NEW
    private lateinit var primaryIcon: ImageView
    private lateinit var recipeImage: ImageView
    private lateinit var ingredientsRecycler: RecyclerView
    private lateinit var stepsRecycler: RecyclerView
    private lateinit var btnUpdate: Button
    private var recipe: Recipe? = null

    companion object {
        private const val TAG = "RecipeViewDetailsScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_view_details_screen)

        initializeViews()
        setupCategoryAdapter()
        handleIntentData()
        setupClickListeners()
        setupDynamicErrorClearing()
    }

    private fun initializeViews() {
        findViewById<TextView>(R.id.headerTitle).text = "View Recipe"

        btnUpdate        = findViewById(R.id.btnUpdateRecipe)
        recipeImage      = findViewById(R.id.recipeImage)
        etTitle          = findViewById(R.id.etRecipeTitle)
        etDesc           = findViewById(R.id.etRecipeDesc)
        etCategory       = findViewById(R.id.etRecipeCategory)
        etCookingTime    = findViewById(R.id.etRecipeCookingTime)   // NEW
        primaryIcon      = findViewById(R.id.primaryIcon)
        tilTitle         = findViewById(R.id.tilRecipeTitle)
        tilDesc          = findViewById(R.id.tilRecipeDesc)
        tilCategory      = findViewById(R.id.tilRecipeCategory)
        tilCookingTime   = findViewById(R.id.tilRecipeCookingTime)  // NEW
        ingredientsRecycler = findViewById(R.id.ingredientsRecycler)
        stepsRecycler       = findViewById(R.id.stepsRecycler)
    }

    private fun setupCategoryAdapter() {
        val categories = arrayOf("Breakfast", "Lunch", "Dinner", "Dessert")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(adapter)
        etCategory.isEnabled = false
        tilCategory.isEnabled = false
    }

    private fun handleIntentData() {
        recipe = intent.getSerializableExtra("recipe") as? Recipe
        recipe?.let {
            setupInitialData(it)
        } ?: run {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        val allowEdit = intent.getBooleanExtra("allow_edit", false)
        if (allowEdit) {
            primaryIcon.setImageResource(android.R.drawable.ic_menu_edit)
            primaryIcon.visibility = View.VISIBLE
            primaryIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.icon_disabled)
        } else {
            primaryIcon.visibility = View.GONE
        }

        primaryIcon.setOnClickListener {
            if (!isCurrentlyEditing) {
                enterEditMode()
                btnUpdate.visibility = View.VISIBLE
            }
        }

        findViewById<ImageView>(R.id.iconShare).setOnClickListener { shareRecipe() }
        findViewById<ImageView>(R.id.iconCopy).setOnClickListener { copyRecipe() }

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
     * Populates all fields including cookingTime and nutrition cards.
     */
    private fun setupInitialData(recipe: Recipe) {
        etCategory.isEnabled = false
        tilCategory.isEnabled = false
        etCategory.setOnClickListener(null)

        etTitle.setText(recipe.title)
        etDesc.setText(recipe.description)
        etCategory.setText(recipe.category, false)
        etCookingTime.setText(if (recipe.cookingTime > 0) "${recipe.cookingTime} mins" else "")

        if (!recipe.imageUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(recipe.imageUri)
                .placeholder(R.drawable.recipe_default)
                .error(R.drawable.recipe_default)
                .centerCrop()
                .into(recipeImage)
        } else {
            recipeImage.setImageResource(R.drawable.recipe_default)
        }

        // ── Nutrition cards ─────────────────────────────────── NEW
        bindNutrition(recipe.nutrition ?: Nutrition())

        ingAdapter = IngredientAdapter(recipe.ingredients.toMutableList(), false)
        ingredientsRecycler.layoutManager = LinearLayoutManager(this)
        ingredientsRecycler.adapter = ingAdapter

        stepAdapter = StepsAdapter(recipe.steps.toMutableList(), false)
        stepsRecycler.layoutManager = LinearLayoutManager(this)
        stepsRecycler.adapter = stepAdapter
    }

    private fun bindNutrition(nutrition: Nutrition) {
        try {
            val total = (nutrition.carbs + nutrition.fat + nutrition.protein)
                .takeIf { it > 0f } ?: 0f

            // Hide nutrition section entirely if no data entered
            val nutritionSection = findViewById<View>(R.id.nutritionSection)
            if (total == 0f && nutrition.calories == 0) {
                nutritionSection?.visibility = View.GONE
                return
            }
            nutritionSection?.visibility = View.VISIBLE

            val safeDivisor = total.takeIf { it > 0f } ?: 1f

            // ── Carbs card ──
            val carbsPct = ((nutrition.carbs / safeDivisor) * 100).toInt()
            findViewById<TextView>(R.id.tvCarbs).text    = "${nutrition.carbs}g"
            findViewById<TextView>(R.id.tvCarbsPct).text = "$carbsPct%"
            findViewById<CircularProgressIndicator>(R.id.progressCarbs).apply {
                max = 100
                setProgressCompat(carbsPct, false)
            }

            // ── Fat card ──
            val fatPct = ((nutrition.fat / safeDivisor) * 100).toInt()
            findViewById<TextView>(R.id.tvFat).text    = "${nutrition.fat}g"
            findViewById<TextView>(R.id.tvFatPct).text = "$fatPct%"
            findViewById<CircularProgressIndicator>(R.id.progressFat).apply {
                max = 100
                setProgressCompat(fatPct, false)
            }

            // ── Protein card ──
            val proteinPct = ((nutrition.protein / safeDivisor) * 100).toInt()
            findViewById<TextView>(R.id.tvProtein).text    = "${nutrition.protein}g"
            findViewById<TextView>(R.id.tvProteinPct).text = "$proteinPct%"
            findViewById<CircularProgressIndicator>(R.id.progressProtein).apply {
                max = 100
                setProgressCompat(proteinPct, false)
            }

            // ── Bottom chips ──
            findViewById<TextView>(R.id.tvCalories).text = "${nutrition.calories} kcal"
            findViewById<TextView>(R.id.tvNetCarbs).text = "${nutrition.netCarbs}g"

        } catch (e: Exception) {
            android.util.Log.e("RecipeViewDetailsScreen", "bindNutrition error: ${e.message}", e)
        }
    }

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
                    holder.tilQty?.error  = "Required"
                }
            }
        }

        recipe?.steps?.forEachIndexed { index, step ->
            if (step.text.trim().isEmpty()) {
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

    private fun setupDynamicErrorClearing() {
        etDesc.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) tilDesc.error = null
        }
        etCategory.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) tilCategory.error = null
        }
    }

    private fun enterEditMode() {
        isCurrentlyEditing = true
        btnUpdate.visibility = View.VISIBLE
        primaryIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.buttontext)
        primaryIcon.setImageResource(android.R.drawable.ic_menu_save)

        etTitle.isEnabled    = false
        etDesc.isEnabled     = true
        etCategory.isEnabled = true
        tilCategory.isEnabled = true
        etDesc.requestFocus()

        ingAdapter.isEditMode   = true
        stepAdapter.isEditable  = true
        ingAdapter.notifyDataSetChanged()
        stepAdapter.notifyDataSetChanged()
    }

    private fun syncDataFromUI() {
        recipe?.let {
            it.description = etDesc.text.toString().trim()
            it.category    = etCategory.text.toString().trim()
        }
    }

    private fun buildFullRecipeText(): String {
        val r = recipe ?: return ""
        val sb = StringBuilder()

        sb.appendLine("🍽 ${r.title}")
        sb.appendLine()
        if (r.description.isNotBlank()) {
            sb.appendLine("📖 ${r.description}")
            sb.appendLine()
        }
        sb.appendLine("⏱ Cooking Time: ${r.cookingTime} mins")
        sb.appendLine("📂 Category: ${r.category}")
        sb.appendLine()

        if (r.ingredients.isNotEmpty()) {
            sb.appendLine("🥗 Ingredients:")
            r.ingredients.forEachIndexed { i, ing ->
                sb.appendLine("  ${i + 1}. ${ing.quantity} ${ing.name}")
            }
            sb.appendLine()
        }

        if (r.steps.isNotEmpty()) {
            sb.appendLine("👨‍🍳 Steps:")
            r.steps.forEachIndexed { i, step ->
                sb.appendLine("  ${i + 1}. ${step.text}")
            }
            sb.appendLine()
        }

        r.nutrition?.let { n ->
            if (n.calories > 0 || n.carbs > 0f || n.fat > 0f || n.protein > 0f) {
                sb.appendLine("📊 Nutrition:")
                if (n.calories > 0) sb.appendLine("  Calories: ${n.calories} kcal")
                if (n.carbs > 0f) sb.appendLine("  Carbs: ${n.carbs}g")
                if (n.fat > 0f) sb.appendLine("  Fat: ${n.fat}g")
                if (n.protein > 0f) sb.appendLine("  Protein: ${n.protein}g")
                sb.appendLine()
            }
        }

        if (!r.imageUri.isNullOrEmpty()) {
            sb.appendLine("📸 ${r.imageUri}")
        }

        sb.appendLine()
        sb.append("— Shared from RecipeBytes")
        return sb.toString()
    }

    private fun shareRecipe() {
        val recipeText = buildFullRecipeText()
        val recipeImageUri = recipe?.imageUri

        if (!recipeImageUri.isNullOrEmpty()) {
            try {
                Glide.with(this)
                    .asBitmap()
                    .load(recipeImageUri)
                    .submit()
                    .get()
                    .let { bitmap ->
                        val cacheDir = File(cacheDir, "shared_images")
                        cacheDir.mkdirs()
                        val file = File(cacheDir, "recipe_share_${System.currentTimeMillis()}.png")
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }
                        val uri = FileProvider.getUriForFile(
                            this,
                            "${packageName}.provider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, recipeText)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(intent, "Share Recipe"))
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Share with image failed, falling back to text", e)
                shareTextOnly(recipeText)
            }
        } else {
            shareTextOnly(recipeText)
        }
    }

    private fun shareTextOnly(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share Recipe"))
    }

    private fun copyRecipe() {
        val text = buildFullRecipeText()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Recipe", text))
        Toast.makeText(this, "Full recipe copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun saveAndExitEditMode() {
        isCurrentlyEditing = false
        recipe?.let {
            RecipeRepository.updateRecipe(this, it.title, it)
        }

        btnUpdate.visibility = View.GONE
        primaryIcon.setImageResource(android.R.drawable.ic_menu_edit)
        primaryIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.icon_disabled)

        etTitle.isEnabled    = false
        etDesc.isEnabled     = false
        etCategory.isEnabled  = false
        tilCategory.isEnabled = false
        etCategory.setOnClickListener(null)

        ingAdapter.isEditMode  = false
        stepAdapter.isEditable = false
        ingAdapter.notifyDataSetChanged()
        stepAdapter.notifyDataSetChanged()
    }
}