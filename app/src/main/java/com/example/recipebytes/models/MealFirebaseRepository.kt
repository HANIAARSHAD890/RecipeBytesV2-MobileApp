package com.example.recipebytes.models

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

/**
 * Handles all Firebase Firestore operations for the meal planner.
 * Structure: users/{userId}/mealPlan/{dateKey}/
 */
object MealFirebaseRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun currentUserId() = auth.currentUser?.uid

    // ── Save meals for a single day ───────────────────────────────────────────

    fun saveDayMeals(
        mealDay: MealDay,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val userId = currentUserId() ?: return onError("Not logged in")

        val data = mapOf(
            "date"      to mealDay.date,
            "day"       to mealDay.day,
            "breakfast" to mealDay.breakfast,
            "lunch"     to mealDay.lunch,
            "dinner"    to mealDay.dinner,
            "dessert"   to mealDay.dessert
        )

        db.collection("users")
            .document(userId)
            .collection("mealPlan")
            .document(mealDay.date)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Save failed") }
    }

    // ── Load all days for a month ─────────────────────────────────────────────

    fun loadMonthMeals(
        monthKey: String,
        onSuccess: (List<MealDay>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val userId = currentUserId() ?: return onError("Not logged in")

        db.collection("users")
            .document(userId)
            .collection("mealPlan")
            .whereGreaterThanOrEqualTo("date", "$monthKey-01")
            .whereLessThanOrEqualTo("date", "$monthKey-31")
            .get()
            .addOnSuccessListener { snapshot ->
                val savedDays = snapshot.documents.mapNotNull { doc ->
                    try {
                        MealDay(
                            date      = doc.getString("date") ?: "",
                            day       = doc.getString("day")  ?: "",
                            breakfast = (doc.get("breakfast") as? List<*>)
                                ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf(),
                            lunch     = (doc.get("lunch") as? List<*>)
                                ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf(),
                            dinner    = (doc.get("dinner") as? List<*>)
                                ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf(),
                            dessert   = (doc.get("dessert") as? List<*>)
                                ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                        )
                    } catch (e: Exception) { null }
                }
                onSuccess(savedDays)
            }
            .addOnFailureListener { onError(it.message ?: "Load failed") }
    }

    // ── Remove a meal from a specific category ────────────────────────────────

    fun removeMeal(
        dateKey: String,
        category: String,
        meal: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val userId = currentUserId() ?: return onError("Not logged in")

        val docRef = db.collection("users")
            .document(userId)
            .collection("mealPlan")
            .document(dateKey)

        docRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val list = (doc.get(category) as? List<*>)
                    ?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                list.remove(meal)
                docRef.update(category, list)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.message ?: "Remove failed") }
            }
        }.addOnFailureListener { onError(it.message ?: "Fetch failed") }
    }

    // ── Build empty days for a month ──────────────────────────────────────────

    fun buildDaysForMonth(monthKey: String): MutableList<MealDay> {
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