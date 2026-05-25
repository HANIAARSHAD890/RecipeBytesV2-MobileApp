package com.example.recipebytes.models

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

object MealRepository {

    private const val PREFS_NAME = "meal_prefs"
    private const val KEY_PREFIX = "meal_plan_"

    private val cache = mutableMapOf<String, MutableList<MealDay>>()

    fun getMealPlanForMonth(context: Context, monthKey: String): MutableList<MealDay> {
        return cache.getOrPut(monthKey) { loadFromDisk(context, monthKey) }
    }

    fun saveMealPlan(context: Context, monthKey: String) {
        saveToDisk(context, monthKey, cache[monthKey] ?: return)
    }

    fun removeMealFromDay(context: Context, monthKey: String, dateKey: String, meal: String) {
        cache[monthKey]?.find { it.date == dateKey }?.meals?.remove(meal)
        saveToDisk(context, monthKey, cache[monthKey] ?: return)
    }

    private fun loadFromDisk(context: Context, monthKey: String): MutableList<MealDay> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREFIX + monthKey, null)

        val saved: MutableList<MealDay>? = if (json != null) {
            val type = object : TypeToken<MutableList<MealDay>>() {}.type
            Gson().fromJson(json, type)
        } else null

        val days = buildDaysForMonth(monthKey)
        saved?.forEach { savedDay ->
            days.find { it.date == savedDay.date }?.meals?.addAll(savedDay.meals)
        }
        return days
    }

    private fun saveToDisk(context: Context, monthKey: String, list: MutableList<MealDay>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREFIX + monthKey, Gson().toJson(list)).apply()
    }

    private fun buildDaysForMonth(monthKey: String): MutableList<MealDay> {
        val (year, month) = monthKey.split("-").map { it.toInt() }
        val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dayNames   = arrayOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
        val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec")

        return (1..daysInMonth).map { d ->
            cal.set(year, month - 1, d)
            val label   = "${dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]}, ${monthNames[month-1]} $d"
            val dateKey = "$year-%02d-%02d".format(month, d)
            MealDay(day = label, date = dateKey)
        }.toMutableList()
    }
}