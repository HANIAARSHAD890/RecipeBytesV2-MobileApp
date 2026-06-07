package com.example.recipebytes.services

import com.example.recipebytes.BuildConfig
import com.example.recipebytes.models.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Holds AI-generated recipe data before conversion to a Recipe object
data class AIRecipeResult(
    val title: String,
    val description: String,
    val cookingTime: String,
    val category: String,
    val ingredients: List<Pair<String, String>>,
    val steps: List<String>
)

// Generates recipes using the Groq AI API
object AIRecipeService {

    private val API_KEY = BuildConfig.GROQ_API_KEY
    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Calls the Groq API to generate a complete recipe from a dish name
    suspend fun generateRecipe(dishName: String): Result<AIRecipeResult> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Generate a detailed recipe for "$dishName".
                    Respond ONLY in this exact JSON format, no extra text, no markdown:
                    {
                      "title": "dish name",
                      "description": "short 1 sentence description",
                      "cookingTime": "X mins",
                      "category": "Breakfast or Lunch or Dinner or Dessert",
                      "ingredients": [
                        {"name": "ingredient1", "quantity": "amount with unit"}
                      ],
                      "steps": [
                        "step 1 description",
                        "step 2 description"
                      ]
                    }
                """.trimIndent()

                val requestBody = JSONObject().apply {
                    put("model", "llama-3.1-8b-instant")
                    put("temperature", 0.7)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a professional chef. Always respond with valid JSON only, no markdown, no extra text.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }.toString()

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("API Error ${response.code}: $responseBody")
                    )
                }

                // Parse Groq response
                val jsonResponse = JSONObject(responseBody)
                val text = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                // Clean markdown if present
                val cleanJson = text
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val recipeJson = JSONObject(cleanJson)

                // Parse ingredients
                val ingredientsArray = recipeJson.getJSONArray("ingredients")
                val ingredients = mutableListOf<Pair<String, String>>()
                for (i in 0 until ingredientsArray.length()) {
                    val ing = ingredientsArray.getJSONObject(i)
                    ingredients.add(Pair(ing.getString("name"), ing.getString("quantity")))
                }

                // Parse steps
                val stepsArray = recipeJson.getJSONArray("steps")
                val steps = mutableListOf<String>()
                for (i in 0 until stepsArray.length()) {
                    steps.add(stepsArray.getString(i))
                }

                // Validate category
                val category = recipeJson.getString("category")
                val safeCategory = if (category in Recipe.CATEGORIES) category else "Lunch"

                Result.success(
                    AIRecipeResult(
                        title       = recipeJson.getString("title"),
                        description = recipeJson.getString("description"),
                        cookingTime = recipeJson.getString("cookingTime"),
                        category    = safeCategory,
                        ingredients = ingredients,
                        steps       = steps
                    )
                )

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}