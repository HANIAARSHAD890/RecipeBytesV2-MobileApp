package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.SuggestResult
import com.example.recipebytes.adapters.SuggestResultAdapter
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.widget.LinearLayout

class SuggestFragment : Fragment() {

    private val selectedIngredients = mutableListOf<String>()
    private lateinit var suggestAdapter: SuggestResultAdapter
    private var allResults = listOf<SuggestResult>()
    private var activeFilter = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_smart_suggest_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.headerTitle).text = "Smart Suggest"

        val autoComplete    = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteIngredient)
        val chipGroup       = view.findViewById<ChipGroup>(R.id.chipGroupIngredients)
        val btnSuggest      = view.findViewById<Button>(R.id.btnSuggest)
        val resultsRecycler = view.findViewById<RecyclerView>(R.id.suggestResultsRecycler)
        val layoutEmpty     = view.findViewById<LinearLayout>(R.id.layoutEmptySuggest)
        val tvResultsCount  = view.findViewById<TextView>(R.id.tvResultsCount)

        // Setup results list FIRST — no dependency on anything
        setupResultsList(resultsRecycler)

        // Shopping list button works immediately
        view.findViewById<LinearLayout>(R.id.btnViewShoppingList).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ShoppingListFragment())
                .addToBackStack(null)
                .commit()
        }

        setupFilterTabs(view)

        RecipeRepository.loadFromFirebase {
            if (!isAdded) return@loadFromFirebase
            setupIngredientInput(autoComplete, chipGroup)
        }

        btnSuggest.setOnClickListener {
            if (selectedIngredients.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Please add at least one ingredient", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            allResults = getSuggestedRecipes()
            applyFilter(layoutEmpty, tvResultsCount)
        }
    }

    private fun setupFilterTabs(view: View) {
        val tabAll      = view.findViewById<TextView>(R.id.tabAll)
        val tabCanMake  = view.findViewById<TextView>(R.id.tabCanMake)
        val tabAlmost   = view.findViewById<TextView>(R.id.tabAlmost)
        val tabMissing  = view.findViewById<TextView>(R.id.tabMissing)
        val layoutEmpty    = view.findViewById<LinearLayout>(R.id.layoutEmptySuggest)
        val tvResultsCount = view.findViewById<TextView>(R.id.tvResultsCount)

        val tabs   = listOf(tabAll, tabCanMake, tabAlmost, tabMissing)
        val labels = listOf("All", "Can Make Now", "Almost There", "Missing Many")

        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                activeFilter = labels[index]
                tabs.forEach { t ->
                    t.setBackgroundResource(R.drawable.tab_unselected_bg)
                    t.setTextColor(ContextCompat.getColor(requireContext(), R.color.textcolor))
                }
                tab.setBackgroundResource(R.drawable.tab_selected_bg)
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.buttontext))
                applyFilter(layoutEmpty, tvResultsCount)
            }
        }
    }

    private fun applyFilter(layoutEmpty: LinearLayout, tvResultsCount: TextView) {
        val filtered = if (activeFilter == "All") allResults
        else allResults.filter { it.label == activeFilter }

        suggestAdapter.updateResults(filtered)

        if (filtered.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            tvResultsCount.text    = ""
            view?.findViewById<TextView>(R.id.tvEmptyMessage)?.text =
                if (allResults.isEmpty()) "Add ingredients above to find matching recipes"
                else "No recipes in this category"
        } else {
            layoutEmpty.visibility = View.GONE
            tvResultsCount.text    =
                "${filtered.size} recipe${if (filtered.size > 1) "s" else ""} found"
        }
    }

    private fun setupIngredientInput(autoComplete: AutoCompleteTextView, chipGroup: ChipGroup) {
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

        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val ingredient = dropdownAdapter.getItem(position) ?: ""
            addIngredientChip(ingredient, chipGroup)
            autoComplete.setText("", false)
        }

        autoComplete.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val input = s.toString()
                if (input.isNotEmpty() && !input.matches(Regex("^[a-zA-Z ]*$"))) {
                    autoComplete.setText(input.replace(Regex("[^a-zA-Z ]"), ""))
                    autoComplete.setSelection(autoComplete.text.length)
                }
            }
        })

        autoComplete.setOnEditorActionListener { _, _, _ ->
            val typed = autoComplete.text.toString().trim()
            if (typed.isNotEmpty() && typed.matches(Regex("^[a-zA-Z ]+$"))) {
                addIngredientChip(typed, chipGroup)
                autoComplete.setText("")
            }
            true
        }
    }

    private fun setupResultsList(resultsRecycler: RecyclerView) {
        suggestAdapter = SuggestResultAdapter(mutableListOf()) { result ->
            val fragment = IngredientComparisonFragment()
            fragment.arguments = Bundle().apply {
                putSerializable("recipe", result.recipe)
                putSerializable("userIngredients", ArrayList(selectedIngredients))
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        resultsRecycler.adapter = suggestAdapter
    }

    private fun addIngredientChip(ingredient: String, chipGroup: ChipGroup) {
        if (selectedIngredients.contains(ingredient.lowercase())) {
            Toast.makeText(requireContext(),
                "$ingredient already added", Toast.LENGTH_SHORT).show()
            return
        }
        selectedIngredients.add(ingredient.lowercase())
        val chip = Chip(requireContext()).apply {
            text = ingredient
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedIngredients.remove(ingredient.lowercase())
                chipGroup.removeView(this)
            }
        }
        chipGroup.addView(chip)
    }

    private fun getSuggestedRecipes(): List<SuggestResult> {
        val results = mutableListOf<SuggestResult>()
        for (recipe in RecipeRepository.getAllRecipes()) {
            val recipeIngNames = recipe.ingredients.map { it.name.lowercase().trim() }
            val matchCount = selectedIngredients.count { selected ->
                recipeIngNames.any { it.contains(selected) || selected.contains(it) }
            }
            val total = recipe.ingredients.size
            if (total == 0 || matchCount == 0) continue

            val percent = matchCount.toFloat() / total.toFloat()
            val label = when {
                percent >= 1.0f -> "Can Make Now"
                percent >= 0.5f -> "Almost There"
                else            -> "Missing Many"
            }
            results.add(SuggestResult(recipe, label, matchCount, total))
        }
        val order = mapOf("Can Make Now" to 0, "Almost There" to 1, "Missing Many" to 2)
        return results.sortedWith(compareBy({ order[it.label] }, { -it.matchCount }))
    }
}