package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.IngredientCompareAdapter
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.ShoppingList
import com.example.recipebytes.models.ShoppingListItem
import com.example.recipebytes.services.ShoppingListService

class IngredientComparisonFragment : Fragment(R.layout.fragment_ingredient_comparison) {

    private lateinit var recipe: Recipe
    private var userIngredients = listOf<String>()
    private var missingIngredients = listOf<Ingredient>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        recipe = arguments?.getSerializable("recipe") as? Recipe ?: return

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        userIngredients = arguments?.getSerializable("userIngredients") as? ArrayList<String>
            ?: emptyList()

        val headerTitle = view.findViewById<TextView>(R.id.headerTitle)
        headerTitle.text = "Ingredient Check"

        val tvTitle      = view.findViewById<TextView>(R.id.tvCompareRecipeTitle)
        val tvSummary    = view.findViewById<TextView>(R.id.tvCompareSummary)
        val rvHave       = view.findViewById<RecyclerView>(R.id.rvHaveIngredients)
        val rvMissing    = view.findViewById<RecyclerView>(R.id.rvMissingIngredients)
        val btnShopping  = view.findViewById<Button>(R.id.btnAddToShoppingList)

        tvTitle.text = recipe.title

        // Split ingredients into have and missing
        val haveIngredients = mutableListOf<Ingredient>()
        val missingList     = mutableListOf<Ingredient>()

        recipe.ingredients.forEach { ingredient ->
            val matched = userIngredients.any {
                ingredient.name.lowercase().contains(it.lowercase()) ||
                        it.lowercase().contains(ingredient.name.lowercase())
            }
            if (matched) haveIngredients.add(ingredient)
            else missingList.add(ingredient)
        }

        missingIngredients = missingList

        tvSummary.text = "You have ${haveIngredients.size} of ${recipe.ingredients.size} ingredients"

        // Setup RecyclerViews
        rvHave.layoutManager    = LinearLayoutManager(requireContext())
        rvHave.adapter          = IngredientCompareAdapter(haveIngredients)
        rvMissing.layoutManager = LinearLayoutManager(requireContext())
        rvMissing.adapter       = IngredientCompareAdapter(missingList)

        // Disable button if nothing missing
        if (missingList.isEmpty()) {
            btnShopping.text      = "✅ You have all ingredients!"
            btnShopping.isEnabled = false
        }

        btnShopping.setOnClickListener {
            saveShoppingList()
        }
    }

    private fun saveShoppingList() {
        val items = missingIngredients.map { ing ->
            ShoppingListItem(
                name        = ing.name,
                quantity    = ing.quantity,
                isChecked   = false,
                recipeTitle = recipe.title
            )
        }

        val shoppingList = ShoppingList(
            recipeTitle = recipe.title,
            items       = items
        )

        ShoppingListService.saveShoppingList(
            shoppingList,
            onSuccess = {
                Toast.makeText(requireContext(),
                    "✅ Shopping list saved!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ShoppingListFragment())
                    .addToBackStack(null)
                    .commit()
            },
            onError = { error ->
                Toast.makeText(requireContext(),
                    "Failed to save: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
}