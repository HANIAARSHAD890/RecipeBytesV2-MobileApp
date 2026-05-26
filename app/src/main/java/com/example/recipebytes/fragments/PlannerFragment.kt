package com.example.recipebytes.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.adapters.MealDayAdapter
import com.example.recipebytes.models.MealDay
import com.example.recipebytes.models.MealRepository
import com.example.recipebytes.models.RecipeRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Fragment that manages the weekly meal plan, allowing users to assign recipes to different days.
 */
class PlannerFragment : Fragment() {

    private lateinit var adapter: MealDayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_meal_planner_screen, container, false)
    }

    /**
     * Refreshes the meal plan data from the repository whenever the fragment is resumed.
     */
    override fun onResume() {
        super.onResume()
        MealRepository.init(requireContext())
        adapter.updateList(MealRepository.getMealPlan().toMutableList())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val headerTitle = view.findViewById<TextView>(R.id.headerTitle)
        headerTitle.text = "Meal Planner"

        RecipeRepository.loadFromFirebase()
        setupRecyclerView(view)
    }

    /**
     * Configures the RecyclerView with the meal day adapter and click listener.
     */
    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.mealPlannerRecycler)

        adapter = MealDayAdapter(
            MealRepository.getMealPlan().toMutableList()
        ) { mealDay ->
            showMealPickerDialog(mealDay)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    /**
     * Displays a BottomSheetDialog to select recipes for a specific day.
     */
    private fun showMealPickerDialog(mealDay: MealDay) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.fragment_meal_popup, null)
        dialog.setContentView(view)

        val tvDialogTitle = view.findViewById<TextView>(R.id.tvDialogDay)
        val autoComplete = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteMeal)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSelectedMeals)
        val btnSaveMeals = view.findViewById<Button>(R.id.btnSaveMeals)
        val tvNoResult = view.findViewById<TextView>(R.id.tvNoResult)

        tvDialogTitle.text = "Add meals for ${mealDay.day}"

        setupMealSelection(autoComplete, chipGroup, mealDay, tvNoResult)

        btnSaveMeals.setOnClickListener {
            handleSaveMeals(dialog, selectedMealsInDialog, mealDay)
        }

        dialog.show()
    }

    private val selectedMealsInDialog = mutableListOf<String>()

    /**
     * Configures the recipe selection logic
     */
    private fun setupMealSelection(
        autoComplete: AutoCompleteTextView,
        chipGroup: ChipGroup,
        mealDay: MealDay,
        tvNoResult: TextView
    ) {
        selectedMealsInDialog.clear()
        val recipeNames = RecipeRepository.getAllRecipes().map { it.title }
        val dropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recipeNames)
        autoComplete.setAdapter(dropdownAdapter)

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                tvNoResult.visibility = if (query.isNotEmpty() && !recipeNames.any { it.contains(query, ignoreCase = true) }) View.VISIBLE else View.GONE
            }
        })

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val selected = dropdownAdapter.getItem(position) ?: ""
            if (!selectedMealsInDialog.contains(selected)) {
                selectedMealsInDialog.add(selected)
                addChipToGroup(selected, chipGroup, mealDay)
            } else {
                Toast.makeText(requireContext(), "$selected already added", Toast.LENGTH_SHORT).show()
            }
            autoComplete.setText("", false)
        }
    }

    /**
     * Adds a meal chip to the dialog's ChipGroup with deletion logic.
     */
    private fun addChipToGroup(text: String, chipGroup: ChipGroup, mealDay: MealDay) {
        val chip = Chip(requireContext())
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            selectedMealsInDialog.remove(text)
            mealDay.meals.remove(text)
            chipGroup.removeView(chip)
            MealRepository.removeMealFromDay(requireContext(), mealDay.day, text)
            adapter.refresh()
        }
        chipGroup.addView(chip)
    }

    /**
     * Saves the selected meals for the day and refreshes the main view.
     */
    private fun handleSaveMeals(dialog: BottomSheetDialog, selectedMeals: List<String>, mealDay: MealDay) {
        if (selectedMeals.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one meal!", Toast.LENGTH_SHORT).show()
            return
        }

        selectedMeals.forEach {
            if (!mealDay.meals.contains(it)) {
                mealDay.meals.add(it)
            }
        }

        MealRepository.saveMealPlan(requireContext())
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "Meals saved for ${mealDay.day}!", Toast.LENGTH_SHORT).show()
        dialog.dismiss()
    }
}
