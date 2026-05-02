package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.google.android.material.textfield.TextInputLayout

/**
 * First step of adding a recipe, collecting basic info such as title, description, and category.
 */
class AddRecipeFragment1 : Fragment(R.layout.activity_add_recipe_fragment1) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val next = view.findViewById<Button>(R.id.btnNext)
        val tilTitle = view.findViewById<TextInputLayout>(R.id.tilTitle)
        val tilDesc = view.findViewById<TextInputLayout>(R.id.tilDesc)
        val editTitle = view.findViewById<EditText>(R.id.editTitle)
        val editDesc = view.findViewById<EditText>(R.id.editDesc)
        val spinnerCategory = view.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        val tilCategory = view.findViewById<TextInputLayout>(R.id.tilCategory)

        setupCategoryDropdown(spinnerCategory)
        setupValidationListeners(editTitle, tilTitle, editDesc, tilDesc, spinnerCategory, tilCategory)

        next.setOnClickListener {
            handleNextButtonClick(editTitle, tilTitle, editDesc, tilDesc, spinnerCategory, tilCategory)
        }
    }

    /**
     * Initializes the category dropdown with predefined values.
     */
    private fun setupCategoryDropdown(spinnerCategory: AutoCompleteTextView) {
        val categories = arrayOf("Breakfast", "Lunch", "Dinner", "Dessert")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        spinnerCategory.setAdapter(adapter)
    }

    /**
     * Attaches text change listeners to clear error states dynamically.
     */
    private fun setupValidationListeners(
        editTitle: EditText, tilTitle: TextInputLayout,
        editDesc: EditText, tilDesc: TextInputLayout,
        spinnerCategory: AutoCompleteTextView, tilCategory: TextInputLayout
    ) {
        editTitle.requestFocus()
        editTitle.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) tilTitle.error = null
        }
        editDesc.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) tilDesc.error = null
        }
        spinnerCategory.addTextChangedListener { text ->
            if (text.toString().trim().isNotEmpty()) tilCategory.error = null
        }
    }

    /**
     * Validates input fields and proceeds to the next step if valid.
     */
    private fun handleNextButtonClick(
        editTitle: EditText, tilTitle: TextInputLayout,
        editDesc: EditText, tilDesc: TextInputLayout,
        spinnerCategory: AutoCompleteTextView, tilCategory: TextInputLayout
    ) {
        val title = editTitle.text.toString().trim()
        val desc = editDesc.text.toString().trim()
        val category = spinnerCategory.text.toString().trim()
        var isValid = true

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
            (activity as? AddRecipeActivity)?.goToStep2(title, desc, category)
        }
    }
}
