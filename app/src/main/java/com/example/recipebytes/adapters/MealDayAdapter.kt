package com.example.recipebytes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebytes.R
import com.example.recipebytes.models.MealDay
import com.example.recipebytes.models.MealFirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class MealDayAdapter(
    private val mealDays: MutableList<MealDay>,
    private val monthKey: String,
    private val onAddClick: (MealDay) -> Unit
) : RecyclerView.Adapter<MealDayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayNumber: TextView      = view.findViewById(R.id.tvDayNumber)
        val tvDayAbbr: TextView        = view.findViewById(R.id.tvDayAbbr)
        val tvDayName: TextView        = view.findViewById(R.id.tvDayName)
        val tvMealCount: TextView      = view.findViewById(R.id.tvMealCount)
        val tvEmpty: TextView          = view.findViewById(R.id.tvEmptyState)
        val btnAdd: MaterialButton     = view.findViewById(R.id.btnAddMeal)
        val rowBreakfast: LinearLayout = view.findViewById(R.id.rowBreakfast)
        val rowLunch: LinearLayout     = view.findViewById(R.id.rowLunch)
        val rowDinner: LinearLayout    = view.findViewById(R.id.rowDinner)
        val rowDessert: LinearLayout   = view.findViewById(R.id.rowDessert)
        val chipBreakfast: ChipGroup   = view.findViewById(R.id.chipBreakfast)
        val chipLunch: ChipGroup       = view.findViewById(R.id.chipLunch)
        val chipDinner: ChipGroup      = view.findViewById(R.id.chipDinner)
        val chipDessert: ChipGroup     = view.findViewById(R.id.chipDessert)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_day, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = mealDays.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mealDay = mealDays[position]

        // Date badge
        val parts  = mealDay.date.split("-")
        val dayNum = if (parts.size == 3) parts[2].trimStart('0').ifEmpty { "1" } else "?"
        val abbr   = mealDay.day.split(",").firstOrNull()?.trim() ?: ""
        holder.tvDayNumber.text = dayNum
        holder.tvDayAbbr.text   = abbr
        holder.tvDayName.text   = mealDay.day

        // Meal count summary
        val total = mealDay.breakfast.size + mealDay.lunch.size +
                mealDay.dinner.size    + mealDay.dessert.size
        holder.tvMealCount.text = if (total > 0)
            "$total meal${if (total > 1) "s" else ""} planned"
        else "No meals planned"

        // Empty state
        holder.tvEmpty.visibility = if (total == 0) View.VISIBLE else View.GONE

        // Category chip groups
        bindCategory(holder.chipBreakfast, holder.rowBreakfast,
            mealDay.breakfast, mealDay, "breakfast", position)
        bindCategory(holder.chipLunch, holder.rowLunch,
            mealDay.lunch, mealDay, "lunch", position)
        bindCategory(holder.chipDinner, holder.rowDinner,
            mealDay.dinner, mealDay, "dinner", position)
        bindCategory(holder.chipDessert, holder.rowDessert,
            mealDay.dessert, mealDay, "dessert", position)

        holder.btnAdd.setOnClickListener { onAddClick(mealDay) }
    }

    private fun bindCategory(
        chipGroup: ChipGroup,
        row: LinearLayout,
        meals: MutableList<String>,
        mealDay: MealDay,
        category: String,
        position: Int
    ) {
        chipGroup.removeAllViews()
        if (meals.isEmpty()) {
            row.visibility = View.GONE
            return
        }
        row.visibility = View.VISIBLE
        for (meal in meals.toList()) {
            val chip = Chip(chipGroup.context)
            chip.text = meal
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                meals.remove(meal)
                // Save to Firebase
                MealFirebaseRepository.saveDayMeals(mealDay)
                notifyItemChanged(position)
            }
            chipGroup.addView(chip)
        }
    }

    fun refresh() { notifyDataSetChanged() }

    fun updateList(newList: MutableList<MealDay>) {
        mealDays.clear()
        mealDays.addAll(newList)
        notifyDataSetChanged()
    }
}