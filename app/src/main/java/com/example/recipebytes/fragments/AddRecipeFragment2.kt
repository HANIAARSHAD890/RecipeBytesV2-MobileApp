package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.adapters.IngredientAdapter
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity

/**
 * Second step of adding a recipe, focused on adding and managing ingredients.
 */
class AddRecipeFragment2 : Fragment(R.layout.activity_add_recipe_fragment2) {

    private val ingredientsList = mutableListOf<Ingredient>()
    private lateinit var adapter: IngredientAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.ingredientsRecycler)
        val addBtn = view.findViewById<Button>(R.id.btnAddIngredient)
        val nextBtn = view.findViewById<Button>(R.id.btnNext2)

        setupRecyclerView(recycler)

        addBtn.setOnClickListener {
            addNewIngredient()
        }

        nextBtn.setOnClickListener {
            validateAndProceed(recycler)
        }
    }

    /**
     * Configures the RecyclerView with the ingredient adapter.
     */
    private fun setupRecyclerView(recycler: RecyclerView) {
        if (ingredientsList.isEmpty()) {
            ingredientsList.add(Ingredient("", ""))
        }
        adapter = IngredientAdapter(ingredientsList, true)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    /**
     * Adds a new blank ingredient to the list and notifies the adapter.
     */
    private fun addNewIngredient() {
        ingredientsList.add(Ingredient("", ""))
        adapter.notifyItemInserted(ingredientsList.size - 1)
    }

    /**
     * Validates ingredient entries and navigates to the next step.
     */
    private fun validateAndProceed(recycler: RecyclerView) {
        var hasError = false

        for (i in 0 until ingredientsList.size) {
            val ingredient = ingredientsList[i]
            if (ingredient.name.trim().isEmpty() || ingredient.quantity.trim().isEmpty()) {
                hasError = true
                val holder = recycler.findViewHolderForAdapterPosition(i) as? IngredientAdapter.ViewHolder
                if (ingredient.name.trim().isEmpty()) holder?.tilName?.error = "Required"
                if (ingredient.quantity.trim().isEmpty()) holder?.tilQty?.error = "Required"
            }
        }

        if (!hasError && ingredientsList.isNotEmpty()) {
            (activity as? AddRecipeActivity)?.goToStep3(ingredientsList)
        } else if (ingredientsList.isEmpty()) {
            addNewIngredient()
        }
    }
}
