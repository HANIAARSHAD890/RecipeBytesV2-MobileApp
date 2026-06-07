package com.example.recipebytes.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Step
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64
import java.io.InputStream

class AddRecipeMethodBottomSheet : BottomSheetDialogFragment() {

    private lateinit var layoutOptions: LinearLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var tvLoadingMsg: TextView

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                cameraImageUri?.let { processImageWithGroq(it) }
            }
        }

        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { processImageWithGroq(it) }
        }

        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(),
                "Camera permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_add_recipe_method, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutOptions = view.findViewById(R.id.layoutOptions)
        layoutLoading = view.findViewById(R.id.layoutLoading)
        tvLoadingMsg  = view.findViewById(R.id.tvLoadingMsg)

        view.findViewById<LinearLayout>(R.id.optionManual).setOnClickListener {
            dismiss()
            startActivity(Intent(requireContext(), AddRecipeActivity::class.java))
        }

        // AI option — toggle expand/collapse
        val layoutAIInput  = view.findViewById<LinearLayout>(R.id.layoutAIInput)
        val tvAIArrow      = view.findViewById<TextView>(R.id.tvAIArrow)
        val etAiDishName   = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAiDishName)
        val btnGenerateAI  = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerateAI)
        val aiLoadingLayout = view.findViewById<LinearLayout>(R.id.aiLoadingLayout)

        view.findViewById<LinearLayout>(R.id.optionAI).setOnClickListener {
            if (layoutAIInput.visibility == android.view.View.GONE) {
                layoutAIInput.visibility = android.view.View.VISIBLE
                tvAIArrow.text = "▲"
                etAiDishName.requestFocus()
            } else {
                layoutAIInput.visibility = android.view.View.GONE
                tvAIArrow.text = "▼"
            }
        }

        btnGenerateAI.setOnClickListener {
            val dishName = etAiDishName.text.toString().trim()
            if (dishName.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Please enter a dish name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            aiLoadingLayout.visibility  = android.view.View.VISIBLE
            btnGenerateAI.isEnabled     = false
            btnGenerateAI.text          = "Generating..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = com.example.recipebytes.services.AIRecipeService.generateRecipe(dishName)
                    withContext(Dispatchers.Main) {
                        aiLoadingLayout.visibility = android.view.View.GONE
                        btnGenerateAI.isEnabled    = true
                        btnGenerateAI.text         = "✨ Generate Recipe"

                        result.onSuccess { recipe ->
                            val ingredients = recipe.ingredients.map {
                                com.example.recipebytes.models.Ingredient(it.first, it.second)
                            }
                            val steps = recipe.steps.map {
                                com.example.recipebytes.models.Step(text = it)
                            }
                            val recipeData = RecipeData(
                                title       = recipe.title,
                                description = recipe.description,
                                category    = recipe.category,
                                cookingTime = recipe.cookingTime,
                                ingredients = ingredients,
                                steps       = steps
                            )
                            dismiss()
                            launchWithRecipe(recipeData)
                        }

                        result.onFailure { error ->
                            Toast.makeText(requireContext(),
                                "❌ AI failed: ${error.message}",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        aiLoadingLayout.visibility = android.view.View.GONE
                        btnGenerateAI.isEnabled    = true
                        btnGenerateAI.text         = "✨ Generate Recipe"
                        Toast.makeText(requireContext(),
                            "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        view.findViewById<LinearLayout>(R.id.optionOCR).setOnClickListener {
            showImageSourceDialog()
        }

        view.findViewById<LinearLayout>(R.id.optionLink).setOnClickListener {
            showLinkDialog()
        }
    }

    // ── Image source dialog ───────────────────────────────────────────────────

    private fun showImageSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Image Source")
            .setItems(arrayOf("📷 Camera", "🖼️ Gallery")) { _, which ->
                if (which == 0) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                        openCamera()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                } else {
                    galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().filesDir,
            "ocr_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    // ── OCR via Groq Vision ───────────────────────────────────────────────────

    private fun processImageWithGroq(uri: Uri) {
        showLoading("Reading recipe from image...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Image = uriToBase64(uri)
                val recipe = callGroqVision(base64Image)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    dismiss()
                    launchWithRecipe(recipe)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(requireContext(),
                        "Could not read recipe: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun uriToBase64(uri: Uri): String {
        val inputStream: InputStream = requireContext().contentResolver.openInputStream(uri)!!
        val bytes = inputStream.readBytes()
        inputStream.close()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun callGroqVision(base64Image: String): RecipeData {
        val url  = URL("https://api.groq.com/openai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${getGroqApiKey()}")
        conn.doOutput = true

        val body = JSONObject().apply {
            put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
            put("temperature", 0.1)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", """
                                Extract the recipe from this image and respond ONLY with this JSON, no explanation:
                                {
                                  "title": "",
                                  "description": "",
                                  "category": "Breakfast|Lunch|Dinner|Dessert",
                                  "cookingTime": "",
                                  "ingredients": [{"name": "", "quantity": ""}],
                                  "steps": ["step1", "step2"]
                                }
                            """.trimIndent())
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
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

        return parseRecipeJson(response)
    }

    // ── Link dialog ───────────────────────────────────────────────────────────

    private fun showLinkDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Paste YouTube or recipe website URL"
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Get Recipe from Link")
            .setView(input)
            .setPositiveButton("Extract Recipe") { dialog, _ ->
                val url = input.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Please enter a URL", Toast.LENGTH_SHORT).show()
                } else {
                    dialog.dismiss()
                    showLoading("Extracting recipe from link...")
                    processLinkWithGroq(url)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processLinkWithGroq(link: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val recipe = callGroqWithLink(link)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    dismiss()
                    launchWithRecipe(recipe)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    // Show FULL error on screen
                    android.widget.Toast.makeText(
                        requireContext(),
                        "FAILED: ${e.javaClass.simpleName}: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun callGroqWithLink(link: String): RecipeData {
        val isYouTube = link.contains("youtube.com") || link.contains("youtu.be")

        val pageContent = if (isYouTube) {
            val videoId = extractYouTubeVideoId(link)
                ?: throw Exception("Invalid YouTube URL")
            fetchYouTubeDescription(videoId)
        } else {
            try {
                val pageConn = URL(link).openConnection() as HttpURLConnection
                pageConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                pageConn.connectTimeout = 10000
                pageConn.readTimeout    = 10000
                val text = pageConn.inputStream.bufferedReader().readText()
                pageConn.disconnect()
                val cleaned = text.replace(Regex("<[^>]*>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(4000)
                android.util.Log.d("LinkRecipe", "Page fetched, length: ${cleaned.length}")
                cleaned
            } catch (e: Exception) {
                android.util.Log.e("LinkRecipe", "Fetch failed: ${e.message}")
                throw Exception("Could not fetch page: ${e.message}")
            }
        }

        val url  = URL("https://api.groq.com/openai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${getGroqApiKey()}")
        conn.doOutput = true

        val body = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("temperature", 0.1)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a recipe extractor. Always respond with only valid JSON, no markdown, no explanation. If content is in Urdu or any other language, translate to English.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", """
                    Extract the recipe from this content and respond ONLY with this exact JSON:
                    {
                      "title": "recipe name",
                      "description": "brief description",
                      "category": "Breakfast",
                      "cookingTime": "30",
                      "ingredients": [
                        {"name": "ingredient name", "quantity": "amount"}
                      ],
                      "steps": [
                        "step 1",
                        "step 2"
                      ]
                    }
                    
                    Content:
                    $pageContent
                """.trimIndent())
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

        android.util.Log.d("LinkRecipe", "Groq response: $response")
        return parseRecipeJson(response)
    }

    // ── Parse Groq response ───────────────────────────────────────────────────

    private fun parseRecipeJson(response: String): RecipeData {
        val json = JSONObject(response)
        val content = json
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val r = JSONObject(content)

        val ingredients = mutableListOf<Ingredient>()
        val ingArray = r.getJSONArray("ingredients")
        for (i in 0 until ingArray.length()) {
            val ing = ingArray.getJSONObject(i)
            ingredients.add(Ingredient(
                name     = ing.optString("name", ""),
                quantity = ing.optString("quantity", "")
            ))
        }

        val steps = mutableListOf<Step>()
        val stepsArray = r.getJSONArray("steps")
        for (i in 0 until stepsArray.length()) {
            steps.add(Step(stepId = i + 1, text = stepsArray.getString(i)))
        }

        return RecipeData(
            title       = r.optString("title", ""),
            description = r.optString("description", ""),
            category    = r.optString("category", "Dinner"),
            cookingTime = r.optString("cookingTime", ""),
            ingredients = ingredients,
            steps       = steps
        )
    }

    // ── Launch AddRecipeActivity with pre-filled data ─────────────────────────

    private fun launchWithRecipe(recipe: RecipeData) {
        android.util.Log.d("LinkRecipe", "Launching with: ${recipe.title}, ingredients: ${recipe.ingredients.size}, steps: ${recipe.steps.size}")
        val intent = Intent(requireContext(), AddRecipeActivity::class.java).apply {
            putExtra("mode",        "prefilled")
            putExtra("title",       recipe.title)
            putExtra("desc",        recipe.description)
            putExtra("category",    recipe.category)
            putExtra("cookingTime", recipe.cookingTime)
            putExtra("ingredients", ArrayList(recipe.ingredients))
            putExtra("steps",       ArrayList(recipe.steps))
        }
        startActivity(intent)
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun showLoading(msg: String) {
        layoutOptions.visibility = View.GONE
        layoutLoading.visibility = View.VISIBLE
        tvLoadingMsg.text        = msg
    }

    private fun hideLoading() {
        layoutOptions.visibility = View.VISIBLE
        layoutLoading.visibility = View.GONE
    }

    private fun getGroqApiKey() = "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"
    private fun getYouTubeApiKey() = "AIzaSyA4nnp7TC496TldfDH-swgSVPmv5FXSP30"

    private fun extractYouTubeVideoId(url: String): String? {
        return when {
            url.contains("youtu.be/") ->
                url.substringAfter("youtu.be/").substringBefore("?")
            url.contains("youtube.com/watch") ->
                url.substringAfter("v=").substringBefore("&")
            url.contains("youtube.com/shorts/") ->
                url.substringAfter("shorts/").substringBefore("?")
            else -> null
        }
    }
    private fun fetchYouTubeDescription(videoId: String): String {
        val apiUrl = "https://www.googleapis.com/youtube/v3/videos?id=$videoId&part=snippet&key=${getYouTubeApiKey()}"
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout    = 10000

        val responseCode = conn.responseCode
        val response = if (responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "No error"
            conn.disconnect()
            throw Exception("YouTube API error: $error")
        }
        conn.disconnect()

        val json    = JSONObject(response)
        val items   = json.getJSONArray("items")
        if (items.length() == 0) throw Exception("Video not found")

        val snippet     = items.getJSONObject(0).getJSONObject("snippet")
        val title       = snippet.optString("title", "")
        val description = snippet.optString("description", "")

        if (description.isEmpty()) throw Exception("No description found in this video")

        return "Video Title: $title\n\nDescription:\n$description"
    }

    data class RecipeData(
        val title: String,
        val description: String,
        val category: String,
        val cookingTime: String,
        val ingredients: List<Ingredient>,
        val steps: List<Step>
    )
}