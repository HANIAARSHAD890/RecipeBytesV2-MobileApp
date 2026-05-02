package com.example.recipebytes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.models.MealDay
import com.example.recipebytes.models.MealRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Adapter for managing and displaying the meal plan for each day of the week.
 */
class MealDayAdapter(
    private val mealDays: MutableList<MealDay>,
    private val onAddClick: (MealDay) -> Unit
) : RecyclerView.Adapter<MealDayAdapter.ViewHolder>() {

    /**
     * ViewHolder class for individual day items in the meal plan.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDayName)
        val chipGroup: ChipGroup = view.findViewById(R.id.chipGroupMeals)
        val btnAdd: Button = view.findViewById(R.id.btnAddMeal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_day, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = mealDays.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mealDay = mealDays[position]
        holder.tvDay.text = mealDay.day

        setupMealChips(holder, mealDay, position)

        holder.btnAdd.setOnClickListener {
            onAddClick(mealDay)
        }
    }

    /**
     * Populates the ChipGroup with meal chips and handles their deletion.
     */
    private fun setupMealChips(holder: ViewHolder, mealDay: MealDay, position: Int) {
        holder.chipGroup.removeAllViews()
        for (meal in mealDay.meals) {
            val chip = Chip(holder.itemView.context)
            chip.text = meal
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                mealDay.meals.remove(meal)
                MealRepository.saveMealPlan(holder.itemView.context)
                notifyItemChanged(position)
            }
            holder.chipGroup.addView(chip)
        }
    }

    /**
     * Refreshes the entire list of meal days.
     */
    fun refresh() {
        notifyDataSetChanged()
    }

    /**
     * Updates the data source and refreshes the adapter.
     */
    fun updateList(newList: MutableList<MealDay>) {
        mealDays.clear()
        mealDays.addAll(newList)
        notifyDataSetChanged()
    }
}
