package com.example.recipebytes.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.recipebytes.BuildConfig
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Nutrition
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.recipebytes.services.DraftService

class AddRecipeFragmentNutrition : Fragment(R.layout.activity_add_recipe_fragment_nutrition) {

    private lateinit var ingredients: List<Ingredient>

    // Views
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutFields: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var etCalories: TextInputEditText
    private lateinit var etProtein: TextInputEditText
    private lateinit var etCarbs: TextInputEditText
    private lateinit var etNetCarbs: TextInputEditText
    private lateinit var etFat: TextInputEditText

    private lateinit var tilCalories: TextInputLayout
    private lateinit var tilProtein: TextInputLayout
    private lateinit var tilCarbs: TextInputLayout
    private lateinit var tilNetCarbs: TextInputLayout
    private lateinit var tilFat: TextInputLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get ingredients passed from activity
        @Suppress("UNCHECKED_CAST")
        ingredients = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            @Suppress("UNCHECKED_CAST")
            arguments?.getSerializable("ingredients", ArrayList::class.java) as? ArrayList<Ingredient>
        } else {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            arguments?.getSerializable("ingredients") as? ArrayList<Ingredient>
        } ?: emptyList()

        bindViews(view)
        fetchNutrition()

        btnNext.setOnClickListener { validateAndProceed() }
    }

    private fun bindViews(view: View) {
        layoutLoading = view.findViewById(R.id.layoutLoading)
        layoutFields  = view.findViewById(R.id.layoutFields)
        btnNext       = view.findViewById(R.id.btnNextNutrition)

        etCalories  = view.findViewById(R.id.etCalories)
        etProtein   = view.findViewById(R.id.etProtein)
        etCarbs     = view.findViewById(R.id.etCarbs)
        etNetCarbs  = view.findViewById(R.id.etNetCarbs)
        etFat       = view.findViewById(R.id.etFat)

        tilCalories = view.findViewById(R.id.tilCalories)
        tilProtein  = view.findViewById(R.id.tilProtein)
        tilCarbs    = view.findViewById(R.id.tilCarbs)
        tilNetCarbs = view.findViewById(R.id.tilNetCarbs)
        tilFat      = view.findViewById(R.id.tilFat)
    }

    private fun fetchNutrition() {
        android.util.Log.d("NutritionFragment", "Ingredients received: ${ingredients.size}")
        ingredients.forEach {
            android.util.Log.d("NutritionFragment", "  - ${it.name}: ${it.quantity}")
        }

        val ingredientText = ingredients.joinToString(", ") {
            "${it.name} ${it.quantity}"
        }

        if (ingredients.isEmpty()) {
            showFallbackFields("No ingredients found — go back and re-enter")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nutrition = callGroqApi(ingredientText)
                withContext(Dispatchers.Main) {
                    populateFields(nutrition)
                    showFields()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showFallbackFields("Error: ${e.message}")
                }
            }
        }
    }

    private fun callGroqApi(ingredientText: String): Nutrition {
        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${getGroqApiKey()}")
        conn.doOutput = true

        val prompt = """
            Given these recipe ingredients: $ingredientText
            
            Calculate the total nutrition for the entire recipe and respond ONLY with a JSON object, no explanation:
            {
              "calories": <integer>,
              "protein": <float in grams>,
              "carbs": <float in grams>,
              "fat": <float in grams>,
              "netCarbs": <float in grams, carbs minus fiber>
            }
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("temperature", 0.1)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a nutrition calculator. Always respond with only valid JSON, no markdown, no explanation.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        conn.outputStream.write(body.toString().toByteArray())
        conn.outputStream.flush()

        val responseCode = conn.responseCode
        val response = if (responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
            conn.disconnect()
            throw Exception("HTTP $responseCode: $error")
        }
        conn.disconnect()

        val json = JSONObject(response)
        val content = json
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            // Strip markdown code fences if model wraps in ```json
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val nutritionJson = JSONObject(content)
        return Nutrition(
            calories = nutritionJson.getInt("calories"),
            protein  = nutritionJson.getDouble("protein").toFloat(),
            carbs    = nutritionJson.getDouble("carbs").toFloat(),
            fat      = nutritionJson.getDouble("fat").toFloat(),
            netCarbs = nutritionJson.getDouble("netCarbs").toFloat()
        )
    }

    private fun getGroqApiKey(): String = BuildConfig.GROQ_API_KEY

    private fun populateFields(nutrition: Nutrition) {
        etCalories.setText(nutrition.calories.toString())
        etProtein.setText(nutrition.protein.toString())
        etCarbs.setText(nutrition.carbs.toString())
        etNetCarbs.setText(nutrition.netCarbs.toString())
        etFat.setText(nutrition.fat.toString())
    }

    private fun showFields() {
        layoutLoading.visibility = View.GONE
        layoutFields.visibility  = View.VISIBLE
        btnNext.isEnabled        = true
    }

    private fun showFallbackFields(errorMsg: String = "") {
        layoutLoading.visibility = View.GONE
        layoutFields.visibility  = View.VISIBLE
        btnNext.isEnabled        = true
        tilCalories.helperText   = if (errorMsg.isNotEmpty()) errorMsg else "Could not calculate · enter manually"
    }

    private fun validateAndProceed() {
        val caloriesStr = etCalories.text.toString().trim()
        val proteinStr  = etProtein.text.toString().trim()
        val carbsStr    = etCarbs.text.toString().trim()
        val netCarbsStr = etNetCarbs.text.toString().trim()
        val fatStr      = etFat.text.toString().trim()

        var hasError = false
        if (caloriesStr.isEmpty()) { tilCalories.error = "Required"; hasError = true }
        else tilCalories.error = null
        if (proteinStr.isEmpty())  { tilProtein.error  = "Required"; hasError = true }
        else tilProtein.error = null
        if (carbsStr.isEmpty())    { tilCarbs.error    = "Required"; hasError = true }
        else tilCarbs.error = null
        if (netCarbsStr.isEmpty()) { tilNetCarbs.error = "Required"; hasError = true }
        else tilNetCarbs.error = null
        if (fatStr.isEmpty())      { tilFat.error      = "Required"; hasError = true }
        else tilFat.error = null

        if (!hasError) {
            val nutrition = Nutrition(
                calories = caloriesStr.toInt(),
                protein  = proteinStr.toFloat(),
                carbs    = carbsStr.toFloat(),
                fat      = fatStr.toFloat(),
                netCarbs = netCarbsStr.toFloat()
            )
            // Auto-save draft with nutrition
            val activity = activity as? AddRecipeActivity
            DraftService.saveDraft(
                title       = activity?.getRecipeTitle()       ?: "",
                desc        = activity?.getRecipeDesc()        ?: "",
                category    = activity?.getRecipeCategory()    ?: "",
                cookingTime = activity?.getRecipeCookingTime() ?: "",
                ingredients = ingredients,
                nutrition   = nutrition,
                lastStep    = 3
            )
            activity?.goToStep3WithNutrition(nutrition)
        }
    }
}