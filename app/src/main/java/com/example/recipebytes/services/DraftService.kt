package com.example.recipebytes.services

import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Nutrition
import com.example.recipebytes.models.Step
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object DraftService {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private fun draftRef() = auth.currentUser?.uid?.let {
        database.child("users").child(it).child("recipeDraft")
    }

    // Save draft after each step
    fun saveDraft(
        title: String = "",
        desc: String = "",
        category: String = "",
        cookingTime: String = "",
        ingredients: List<Ingredient> = emptyList(),
        nutrition: Nutrition? = null,
        steps: List<Step> = emptyList(),
        lastStep: Int = 1
    ) {
        val ref = draftRef() ?: return

        val ingredientsMap = mutableMapOf<String, Map<String, String>>()
        ingredients.forEachIndexed { index, ing ->
            ingredientsMap[index.toString()] = mapOf(
                "name" to ing.name,
                "quantity" to ing.quantity
            )
        }

        val stepsMap = mutableMapOf<String, Map<String, Any>>()
        steps.forEachIndexed { index, step ->
            stepsMap[index.toString()] = mapOf(
                "stepId" to (index + 1),
                "text" to step.text
            )
        }

        val draft = mutableMapOf<String, Any>(
            "title" to title,
            "desc" to desc,
            "category" to category,
            "cookingTime" to cookingTime,
            "lastStep" to lastStep,
            "ingredients" to ingredientsMap,
            "steps" to stepsMap
        )

        nutrition?.let {
            draft["nutrition"] = mapOf(
                "calories" to it.calories,
                "protein" to it.protein,
                "carbs" to it.carbs,
                "fat" to it.fat,
                "netCarbs" to it.netCarbs
            )
        }

        ref.setValue(draft)
    }

    // Load draft from Firebase
    fun loadDraft(onResult: (DraftData?) -> Unit) {
        val ref = draftRef() ?: run { onResult(null); return }

        ref.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) { onResult(null); return@addOnSuccessListener }

                val title       = snapshot.child("title").value as? String ?: ""
                val desc        = snapshot.child("desc").value as? String ?: ""
                val category    = snapshot.child("category").value as? String ?: ""
                val cookingTime = snapshot.child("cookingTime").value as? String ?: ""
                val lastStepRaw = snapshot.child("lastStep").value
                val lastStep    = when (lastStepRaw) {
                    is Long -> lastStepRaw.toInt()
                    is Int  -> lastStepRaw
                    else    -> 1
                }

                val ingredients = mutableListOf<Ingredient>()
                for (ing in snapshot.child("ingredients").children) {
                    val name     = ing.child("name").value as? String ?: ""
                    val quantity = ing.child("quantity").value as? String ?: ""
                    ingredients.add(Ingredient(name, quantity))
                }

                val steps = mutableListOf<Step>()
                for (step in snapshot.child("steps").children) {
                    val text = step.child("text").value as? String ?: ""
                    val sidRaw = step.child("stepId").value
                    val sid = when (sidRaw) {
                        is Long -> sidRaw.toInt()
                        is Int  -> sidRaw
                        else    -> 0
                    }
                    steps.add(Step(stepId = sid, text = text))
                }

                var nutrition: Nutrition? = null
                val ns = snapshot.child("nutrition")
                if (ns.exists()) {
                    val calRaw = ns.child("calories").value
                    val calories = when (calRaw) {
                        is Long -> calRaw.toInt()
                        is Int  -> calRaw
                        else    -> 0
                    }
                    nutrition = Nutrition(
                        calories = calories,
                        protein  = (ns.child("protein").value as? Number)?.toFloat()  ?: 0f,
                        carbs    = (ns.child("carbs").value as? Number)?.toFloat()    ?: 0f,
                        fat      = (ns.child("fat").value as? Number)?.toFloat()      ?: 0f,
                        netCarbs = (ns.child("netCarbs").value as? Number)?.toFloat() ?: 0f
                    )
                }

                // Only return draft if at least title is filled
                if (title.isEmpty()) { onResult(null); return@addOnSuccessListener }

                onResult(DraftData(
                    title, desc, category, cookingTime,
                    ingredients, nutrition, steps, lastStep
                ))
            }
            .addOnFailureListener { onResult(null) }
    }

    // Delete draft after recipe is saved
    fun clearDraft() {
        draftRef()?.removeValue()
    }

    data class DraftData(
        val title: String,
        val desc: String,
        val category: String,
        val cookingTime: String,
        val ingredients: List<Ingredient>,
        val nutrition: Nutrition?,
        val steps: List<Step>,
        val lastStep: Int
    )
}