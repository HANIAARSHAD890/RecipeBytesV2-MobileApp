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
import com.example.recipebytes.services.DraftService

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

    private fun setupRecyclerView(recycler: RecyclerView) {
        if (ingredientsList.isEmpty()) {
            val aiIngredients = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                @Suppress("UNCHECKED_CAST")
                arguments?.getSerializable("aiIngredients", ArrayList::class.java) as? ArrayList<Ingredient>
            } else {
                @Suppress("UNCHECKED_CAST", "DEPRECATION")
                arguments?.getSerializable("aiIngredients") as? ArrayList<Ingredient>
            }

            if (!aiIngredients.isNullOrEmpty()) {
                ingredientsList.addAll(aiIngredients)
            } else {
                ingredientsList.add(Ingredient("", ""))
            }
        }
        adapter = IngredientAdapter(ingredientsList, true)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    private fun addNewIngredient() {
        ingredientsList.add(Ingredient("", ""))
        adapter.notifyItemInserted(ingredientsList.size - 1)
    }

    /**
     * Validates ingredient entries and navigates to the next step.
     *
     * Quantity accepts any non-empty string — "500g", "2 cups", "a handful", etc.
     * Structured parsing is deferred to the smart suggest screen.
     */
    private fun validateAndProceed(recycler: RecyclerView) {
        recycler.focusedChild?.clearFocus()

        var hasError = false
        for (i in ingredientsList.indices) {
            val ingredient = ingredientsList[i]
            val nameBlank  = ingredient.name.trim().isEmpty()
            val qtyBlank   = ingredient.quantity.trim().isEmpty()
            if (nameBlank || qtyBlank) {
                hasError = true
                val holder = recycler.findViewHolderForAdapterPosition(i)
                        as? IngredientAdapter.ViewHolder
                if (nameBlank) holder?.tilName?.error = "Required"
                if (qtyBlank)  holder?.tilQty?.error  = "Enter a quantity (e.g. 500g, 2 cups)"
            }
        }

        if (!hasError && ingredientsList.isNotEmpty()) {
            // Auto-save draft with ingredients
            val activity = activity as? AddRecipeActivity
            DraftService.saveDraft(
                title       = activity?.getRecipeTitle()       ?: "",
                desc        = activity?.getRecipeDesc()        ?: "",
                category    = activity?.getRecipeCategory()    ?: "",
                cookingTime = activity?.getRecipeCookingTime() ?: "",
                ingredients = ingredientsList,
                lastStep    = 2
            )
            activity?.goToStep3(ingredientsList)
        } else if (ingredientsList.isEmpty()) {
            addNewIngredient()
        }
    }
}