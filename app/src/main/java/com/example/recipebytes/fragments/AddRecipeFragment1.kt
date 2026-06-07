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
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.Step
import com.example.recipebytes.services.AIRecipeService
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import com.example.recipebytes.services.DraftService

class AddRecipeFragment1 : Fragment(R.layout.activity_add_recipe_fragment1) {

    private var aiIngredients = listOf<Ingredient>()
    private var aiSteps       = listOf<Step>()
    private var aiGenerated   = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNext         = view.findViewById<MaterialButton>(R.id.btnNext)
        val tilTitle        = view.findViewById<TextInputLayout>(R.id.tilTitle)
        val tilDesc         = view.findViewById<TextInputLayout>(R.id.tilDesc)
        val editTitle       = view.findViewById<TextInputEditText>(R.id.editTitle)
        val editDesc        = view.findViewById<TextInputEditText>(R.id.editDesc)
        val editCookTime    = view.findViewById<TextInputEditText>(R.id.editCookingTime)
        val spinnerCategory = view.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        val tilCategory     = view.findViewById<TextInputLayout>(R.id.tilCategory)


        setupCategoryDropdown(spinnerCategory)
        setupValidationListeners(editTitle, tilTitle, editDesc, tilDesc,
            spinnerCategory, tilCategory)

        // Check for existing draft on open
        checkForDraft(editTitle, editDesc, editCookTime, spinnerCategory)

        // Pre-fill from OCR or Link
        // Pre-fill from OCR or Link
        val prefillTitle = arguments?.getString("prefillTitle")
        if (!prefillTitle.isNullOrEmpty()) {
            editTitle.setText(prefillTitle)
            editDesc.setText(arguments?.getString("prefillDesc") ?: "")
            editCookTime.setText(arguments?.getString("prefillCookingTime") ?: "")
            spinnerCategory.setText(arguments?.getString("prefillCategory") ?: "", false)

            // Read ingredients and steps directly from bundle — reliable
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            val prefillIngredients = arguments?.getSerializable("prefillIngredients") as? ArrayList<Ingredient>
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            val prefillSteps = arguments?.getSerializable("prefillSteps") as? ArrayList<Step>

            if (!prefillIngredients.isNullOrEmpty()) {
                aiIngredients = prefillIngredients
                aiSteps       = prefillSteps ?: emptyList()
                aiGenerated   = true
            }
        }
        // Next
        btnNext.setOnClickListener {
            handleNext(editTitle, tilTitle, editDesc, tilDesc,
                spinnerCategory, tilCategory, editCookTime)
        }
    }

    // Check Firebase for an existing draft and prompt user
    private fun checkForDraft(
        editTitle: TextInputEditText,
        editDesc: TextInputEditText,
        editCookTime: TextInputEditText,
        spinnerCategory: AutoCompleteTextView
    ) {
        DraftService.loadDraft { draft ->
            if (draft == null) return@loadDraft
            // Show dialog on main thread
            activity?.runOnUiThread {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Continue Draft?")
                    .setMessage("You have an unfinished recipe \"${draft.title}\". Continue where you left off?")
                    .setPositiveButton("Continue") { _, _ ->
                        // Restore Step 1 fields
                        editTitle.setText(draft.title)
                        editDesc.setText(draft.desc)
                        editCookTime.setText(draft.cookingTime)
                        spinnerCategory.setText(draft.category, false)

                        // If draft has ingredients, skip straight to the right step
                        val activity = activity as? AddRecipeActivity ?: return@setPositiveButton
                        when {
                            draft.lastStep >= 3 && draft.steps.isNotEmpty() -> {
                                // Go straight to steps screen with saved data
                                activity.goToStep3FromDraft(
                                    draft.title, draft.desc, draft.category,
                                    draft.cookingTime, draft.ingredients,
                                    draft.nutrition
                                )
                            }
                            draft.lastStep >= 2 && draft.ingredients.isNotEmpty() -> {
                                // Go straight to nutrition screen
                                activity.goToStep2FromDraft(
                                    draft.title, draft.desc, draft.category,
                                    draft.cookingTime, draft.ingredients
                                )
                            }
                            else -> {
                                // Just fill the fields, user continues from step 1
                            }
                        }
                    }
                    .setNegativeButton("Start Fresh") { _, _ ->
                        DraftService.clearDraft()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun setupCategoryDropdown(spinnerCategory: AutoCompleteTextView) {
        val adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_1, Recipe.CATEGORIES)
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

        if (title.isEmpty())    { tilTitle.error    = "Title cannot be blank";       isValid = false }
        if (desc.isEmpty())     { tilDesc.error     = "Description cannot be blank"; isValid = false }
        if (category.isEmpty()) { tilCategory.error = "Please select a category";    isValid = false }

        if (isValid) {
            // Auto-save draft for step 1
            DraftService.saveDraft(
                title = title, desc = desc,
                category = category, cookingTime = cookingTime,
                lastStep = 1
            )

            val activity = activity as? AddRecipeActivity ?: return
            if (aiGenerated && aiIngredients.isNotEmpty()) {
                // Save AI data to activity first, then go through normal flow
                // so user can review/edit ingredients, nutrition, and steps
                activity.goToStep2WithAIEditable(title, desc, category, cookingTime,
                    aiIngredients, aiSteps)
            } else {
                activity.goToStep2(title, desc, category, cookingTime)
            }

        }
    }
}