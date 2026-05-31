package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.SuggestResult
import com.example.recipebytes.adapters.SuggestResultAdapter
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SuggestFragment : Fragment() {

    private val selectedIngredients = mutableListOf<String>()
    private lateinit var suggestAdapter: SuggestResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_smart_suggest_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val headerTitle   = view.findViewById<TextView>(R.id.headerTitle)
        headerTitle.text  = "Smart Suggest"

        val autoComplete  = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteIngredient)
        val chipGroup     = view.findViewById<ChipGroup>(R.id.chipGroupIngredients)
        val btnSuggest    = view.findViewById<Button>(R.id.btnSuggest)
        val resultsRecycler = view.findViewById<RecyclerView>(R.id.suggestResultsRecycler)

        setupResultsList(resultsRecycler)

        RecipeRepository.loadFromFirebase {
            if (!isAdded) return@loadFromFirebase
            setupIngredientInput(autoComplete, chipGroup)
        }

        btnSuggest.setOnClickListener {
            handleSuggestClick()
        }
    }

    private fun setupIngredientInput(
        autoComplete: AutoCompleteTextView,
        chipGroup: ChipGroup
    ) {
        val allIngredientNames = RecipeRepository.getAllRecipes()
            .flatMap { it.ingredients }
            .map { it.name }
            .distinct()
            .sorted()

        val dropdownAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            allIngredientNames
        )
        autoComplete.setAdapter(dropdownAdapter)
        autoComplete.threshold = 1

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val ingredient = dropdownAdapter.getItem(position) ?: ""
            addIngredientChip(ingredient, chipGroup)
            autoComplete.setText("", false)
        }

        setupInputValidation(autoComplete, chipGroup)
    }

    private fun setupInputValidation(
        autoComplete: AutoCompleteTextView,
        chipGroup: ChipGroup
    ) {
        autoComplete.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val input = s.toString()
                if (input.isNotEmpty() && !input.matches(Regex("^[a-zA-Z ]*$"))) {
                    autoComplete.setText(input.replace(Regex("[^a-zA-Z ]"), ""))
                    autoComplete.setSelection(autoComplete.text.length)
                    Toast.makeText(requireContext(),
                        "Only letters are allowed", Toast.LENGTH_SHORT).show()
                }
            }
        })

        autoComplete.setOnEditorActionListener { _, _, _ ->
            val typed = autoComplete.text.toString().trim()
            if (typed.isNotEmpty() && typed.matches(Regex("^[a-zA-Z ]+$"))) {
                addIngredientChip(typed, chipGroup)
                autoComplete.setText("")
            } else if (typed.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Please enter an ingredient", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun setupResultsList(resultsRecycler: RecyclerView) {
        suggestAdapter = SuggestResultAdapter(mutableListOf()) { result ->
            // Navigate to ingredient comparison screen
            val fragment = IngredientComparisonFragment()
            val bundle   = Bundle().apply {
                putSerializable("recipe", result.recipe)
                putSerializable("userIngredients", ArrayList(selectedIngredients))
            }
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        resultsRecycler.adapter = suggestAdapter
    }

    private fun handleSuggestClick() {
        if (selectedIngredients.isEmpty()) {
            Toast.makeText(requireContext(),
                "Please add at least one ingredient", Toast.LENGTH_SHORT).show()
            return
        }
        val results = getSuggestedRecipes()
        if (results.isEmpty()) {
            Toast.makeText(requireContext(),
                "No matching recipes found", Toast.LENGTH_SHORT).show()
        }
        suggestAdapter.updateResults(results)
    }

    private fun addIngredientChip(ingredient: String, chipGroup: ChipGroup) {
        if (selectedIngredients.contains(ingredient.lowercase())) {
            Toast.makeText(requireContext(),
                "$ingredient already added", Toast.LENGTH_SHORT).show()
            return
        }
        selectedIngredients.add(ingredient.lowercase())

        val chip = Chip(requireContext())
        chip.text = ingredient
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            selectedIngredients.remove(ingredient.lowercase())
            chipGroup.removeView(chip)
            if (selectedIngredients.isEmpty()) {
                suggestAdapter.updateResults(emptyList())
            } else {
                suggestAdapter.updateResults(getSuggestedRecipes())
            }
        }
        chipGroup.addView(chip)
    }

    private fun getSuggestedRecipes(): List<SuggestResult> {
        val recipes = RecipeRepository.getAllRecipes()
        val results = mutableListOf<SuggestResult>()

        for (recipe in recipes) {
            val recipeIngredientNames = recipe.ingredients.map { it.name.lowercase() }
            val matchCount = selectedIngredients.count { selected ->
                recipeIngredientNames.any {
                    it.contains(selected) || selected.contains(it)
                }
            }
            val total = recipe.ingredients.size
            if (total == 0) continue

            val matchPercent = matchCount.toDouble() / total.toDouble()
            val label = when {
                matchPercent >= 0.7 -> "Best Match"
                matchPercent >= 0.4 -> "Better Match"
                matchCount > 0      -> "Less Likely"
                else                -> continue
            }
            results.add(SuggestResult(recipe, label, matchCount, total))
        }
        return results.sortedWith(compareByDescending { it.matchCount })
    }
}