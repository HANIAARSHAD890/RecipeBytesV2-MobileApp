package com.example.recipebytes.models

import java.io.Serializable

/**
 * Data class representing a specific day in the meal planner and its associated meals.
 * 
 * @property day The name of the day (e.g., "Monday").
 * @property meals A mutable list of recipe titles planned for this day.
 */
data class MealDay(
    val day: String,
    val meals: MutableList<String> = mutableListOf()
) : Serializable
