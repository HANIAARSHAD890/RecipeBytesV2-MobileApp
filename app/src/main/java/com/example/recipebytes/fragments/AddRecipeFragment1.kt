package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Step
import com.example.recipebytes.services.AIRecipeService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AddRecipeFragment1 : Fragment(R.layout.activity_add_recipe_fragment1) {

    // Stores AI generated ingredients and steps to pass forward
    private var aiIngredients = listOf<Ingredient>()
    private var aiSteps       = listOf<Step>()
    private var aiGenerated   = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNext        = view.findViewById<MaterialButton>(R.id.btnNext)
        val tilTitle       = view.findViewById<TextInputLayout>(R.id.tilTitle)
        val tilDesc        = view.findViewById<TextInputLayout>(R.id.tilDesc)
        val editTitle      = view.findViewById<TextInputEditText>(R.id.editTitle)
        val editDesc       = view.findViewById<TextInputEditText>(R.id.editDesc)
        val editCookTime   = view.findViewById<TextInputEditText>(R.id.editCookingTime)
        val spinnerCategory = view.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        val tilCategory    = view.findViewById<TextInputLayout>(R.id.tilCategory)

        // AI views
        val editAiDishName  = view.findViewById<TextInputEditText>(R.id.editAiDishName)
        val btnGenerateAI   = view.findViewById<MaterialButton>(R.id.btnGenerateAI)
        val aiLoadingLayout = view.findViewById<LinearLayout>(R.id.aiLoadingLayout)

        setupCategoryDropdown(spinnerCategory)
        setupValidationListeners(editTitle, tilTitle, editDesc, tilDesc,
            spinnerCategory, tilCategory)

        // ── AI Generate button ────────────────────────────────────────────────
        btnGenerateAI.setOnClickListener {
            val dishName = editAiDishName.text.toString().trim()
            if (dishName.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Please enter a dish name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading
            aiLoadingLayout.visibility = View.VISIBLE
            btnGenerateAI.isEnabled    = false
            btnGenerateAI.text         = "Generating..."

            lifecycleScope.launch {
                val result = AIRecipeService.generateRecipe(dishName)

                // Hide loading
                aiLoadingLayout.visibility = View.GONE
                btnGenerateAI.isEnabled    = true
                btnGenerateAI.text         = "Generate"

                result.onSuccess { recipe ->
                    // Fill all fields automatically
                    editTitle.setText(recipe.title)
                    editDesc.setText(recipe.description)
                    editCookTime.setText(recipe.cookingTime)
                    spinnerCategory.setText(recipe.category, false)

                    // Store ingredients and steps for later steps
                    aiIngredients = recipe.ingredients.map {
                        Ingredient(it.first, it.second)
                    }
                    aiSteps = recipe.steps.map { Step(it) }
                    aiGenerated = true

                    Toast.makeText(requireContext(),
                        "✅ Recipe generated! Review and tap Next.",
                        Toast.LENGTH_LONG).show()
                }

                result.onFailure { error ->
                    Toast.makeText(requireContext(),
                        "❌ AI failed: ${error.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }

        // ── Next button ───────────────────────────────────────────────────────
        btnNext.setOnClickListener {
            handleNext(editTitle, tilTitle, editDesc, tilDesc,
                spinnerCategory, tilCategory, editCookTime)
        }
    }

    private fun setupCategoryDropdown(spinnerCategory: AutoCompleteTextView) {
        val categories = arrayOf("Breakfast", "Lunch", "Dinner", "Dessert")
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1, categories)
        spinnerCategory.setAdapter(adapter)
    }

    private fun setupValidationListeners(
        editTitle: TextInputEditText, tilTitle: TextInputLayout,
        editDesc: TextInputEditText, tilDesc: TextInputLayout,
        spinnerCategory: AutoCompleteTextView, tilCategory: TextInputLayout
    ) {
        editTitle.addTextChangedListener {
            if (it.toString().trim().isNotEmpty()) tilTitle.error = null
        }
        editDesc.addTextChangedListener {
            if (it.toString().trim().isNotEmpty()) tilDesc.error = null
        }
        spinnerCategory.addTextChangedListener {
            if (it.toString().trim().isNotEmpty()) tilCategory.error = null
        }
    }

    private fun handleNext(
        editTitle: TextInputEditText, tilTitle: TextInputLayout,
        editDesc: TextInputEditText, tilDesc: TextInputLayout,
        spinnerCategory: AutoCompleteTextView, tilCategory: TextInputLayout,
        editCookTime: TextInputEditText
    ) {
        val title       = editTitle.text.toString().trim()
        val desc        = editDesc.text.toString().trim()
        val category    = spinnerCategory.text.toString().trim()
        val cookingTime = editCookTime.text.toString().trim()
        var isValid     = true

        if (title.isEmpty()) {
            tilTitle.error = "Title cannot be blank"
            isValid = false
        }
        if (desc.isEmpty()) {
            tilDesc.error = "Description cannot be blank"
            isValid = false
        }
        if (category.isEmpty()) {
            tilCategory.error = "Please select a category"
            isValid = false
        }

        if (isValid) {
            val activity = activity as? AddRecipeActivity ?: return

            if (aiGenerated && aiIngredients.isNotEmpty()) {
                // AI generated — skip ingredient and steps screens
                // go straight to step 4 with all data
                activity.goToStep2WithAI(
                    title, desc, category, cookingTime,
                    aiIngredients, aiSteps
                )
            } else {
                // Manual — normal flow
                activity.goToStep2(title, desc, category, cookingTime)
            }
        }
    }
}