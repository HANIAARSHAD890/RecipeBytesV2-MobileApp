package com.example.recipebytes.models

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson

/**
 * Repository for managing the weekly meal plan.
 * Handles persistence using SharedPreferences and JSON serialization.
 */
object MealRepository {

    private const val PREFS_NAME = "meal_prefs"
    private const val KEY_MEALS = "meal_plan_list"
    
    /**
     * The in-memory list representing the weekly meal plan.
     * Initialized with empty meals for each day of the week.
     */
    private val mealPlan = mutableListOf(
        MealDay("Monday"),
        MealDay("Tuesday"),
        MealDay("Wednesday"),
        MealDay("Thursday"),
        MealDay("Friday"),
        MealDay("Saturday"),
        MealDay("Sunday")
    )

    /**
     * Initializes the repository by loading saved meal data from disk.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEALS, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<MealDay>>() {}.type
            val saved = Gson().fromJson<MutableList<MealDay>>(json, type)
            
            // Merge saved meals into the default days to preserve the day order
            mealPlan.forEach { day ->
                val savedDay = saved.find { it.day == day.day }
                if (savedDay != null) {
                    day.meals.clear()
                    day.meals.addAll(savedDay.meals)
                }
            }
        }
    }

    /**
     * Persists the current meal plan to SharedPreferences.
     * 
     * @param context Application context.
     */
    private fun saveToDisk(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(mealPlan)
        prefs.edit().putString(KEY_MEALS, json).apply()
    }

    /**
     * Public method to save the meal plan.
     */
    fun saveMealPlan(context: Context) {
        saveToDisk(context)
    }

    /**
     * Removes a specific meal entry from a given day.
     */
    fun removeMealFromDay(context: Context, dayName: String, meal: String) {
        mealPlan.find { it.day == dayName }?.meals?.remove(meal)
        saveToDisk(context)
    }

    /**
     * Returns the current weekly meal plan.
     */
    fun getMealPlan(): List<MealDay> = mealPlan
    fun getMealsForDay(dayName: String): List<String> {
        return mealPlan.find { it.day == dayName }?.meals ?: emptyList()
    }

}
