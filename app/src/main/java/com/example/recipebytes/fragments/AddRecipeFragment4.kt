package com.example.recipebytes.fragments

import android.net.Uri
import android.os.Bundle
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.DataSource
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.R
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.models.Step
import java.io.File
import java.io.FileOutputStream

/**
 * Final step of adding a recipe, focused on image selection (gallery or URL) and saving the recipe.
 */
class AddRecipeFragment4 : Fragment(R.layout.activity_add_recipe_fragment4) {
    private var imageUri2: Uri? = null
    private lateinit var pickImage: ActivityResultLauncher<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tilUrl = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilImageUrl)
        val pickBtn = view.findViewById<Button>(R.id.btnPickImage)
        val urlInput = view.findViewById<EditText>(R.id.editImageUrl)
        val saveBtn = view.findViewById<Button>(R.id.btnSave)
        val imagePreview = view.findViewById<ImageView>(R.id.imagePreview)

        val title = arguments?.getString("title") ?: ""
        val desc = arguments?.getString("desc") ?: ""
        val category = arguments?.getString("category") ?: ""
        val ingredients = arguments?.getSerializable("ingredients") as? ArrayList<Ingredient> ?: arrayListOf()
        val steps = arguments?.getSerializable("steps") as? ArrayList<Step> ?: arrayListOf()

        checkConnectivity(tilUrl)
        setupImagePicker(imagePreview)

        pickBtn.setOnClickListener {
            pickImage.launch("image/*")
            tilUrl.error = null
        }

        setupUrlInputValidation(urlInput, tilUrl, imagePreview)

        saveBtn.setOnClickListener {
            handleSaveAction(urlInput, tilUrl, title, desc, category, ingredients, steps)
        }
    }

    /**
     * Checks for internet availability and updates UI helper text if offline.
     */
    private fun checkConnectivity(tilUrl: com.google.android.material.textfield.TextInputLayout) {
        if (!isInternetAvailable()) {
            showNoInternetNotification()
            tilUrl.helperText = "⚠️ No internet! Image URLs won't load. Use gallery instead."
        }
    }

    /**
     * Registers the activity result launcher for gallery image selection.
     */
    private fun setupImagePicker(imagePreview: ImageView) {
        pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri2 = it
                Glide.with(this).load(it).into(imagePreview)
            }
        }
    }

    /**
     * Configures validation and preview logic for the image URL input field.
     */
    private fun setupUrlInputValidation(
        urlInput: EditText,
        tilUrl: com.google.android.material.textfield.TextInputLayout,
        imagePreview: ImageView
    ) {
        urlInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = urlInput.text.toString()
                if (url.isNotEmpty()) {
                    if (isInternetAvailable()) {
                        previewImageUrl(url, tilUrl, imagePreview)
                    } else {
                        tilUrl.error = "No internet connection. Image URL may not load."
                    }
                }
            }
        }

        urlInput.addTextChangedListener {
            if (it.toString().trim().isNotEmpty()) {
                tilUrl.error = null
            }
        }
    }

    /**
     * Attempts to load an image from a URL into the preview view.
     */
    private fun previewImageUrl(
        url: String,
        tilUrl: com.google.android.material.textfield.TextInputLayout,
        imagePreview: ImageView
    ) {
        Glide.with(this)
            .load(url)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    tilUrl.error = "Invalid or unreachable image URL"
                    return false
                }
                override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    tilUrl.error = null
                    return false
                }
            })
            .into(imagePreview)
    }

    /**
     * Validates input, saves image locally if needed, and stores the new recipe.
     */
    private fun handleSaveAction(
        urlInput: EditText,
        tilUrl: com.google.android.material.textfield.TextInputLayout,
        title: String,
        desc: String,
        category: String,
        ingredients: ArrayList<Ingredient>,
        steps: ArrayList<Step>
    ) {
        val url = urlInput.text.toString().trim()

        if (imageUri2 == null && url.isEmpty()) {
            tilUrl.error = "Please pick an image from gallery or enter a URL"
            urlInput.requestFocus()
            return
        }

        if (imageUri2 == null && url.isNotEmpty() && !isInternetAvailable()) {
            tilUrl.error = "No internet. Image URL cannot be loaded."
            showNoInternetNotification()
            return
        }

        val finalImagePath = if (imageUri2 != null) {
            saveImageToInternalStorage(imageUri2!!)
        } else {
            url
        }

        val recipe = Recipe(
            title = title,
            description = desc,
            category = category,
            imageUri = finalImagePath,
            ingredients = ingredients,
            steps = steps
        )
        RecipeRepository.addRecipe(requireContext(), recipe)
        Toast.makeText(requireContext(), "Recipe added successfully", Toast.LENGTH_SHORT).show()
        requireActivity().finish()
    }

    /**
     * Helper to check if the device has an active internet connection.
     */
    private fun isInternetAvailable(): Boolean {
        val cm = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**  SYSTEM_BROADCAST_NOTIFICATION
     * Displays a system notification when no internet connection is detected.
     */
    private fun showNoInternetNotification() {
        val channelId = "internet_alert_channel"
        val notificationManager = requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Internet Alerts", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("No Internet Connection ⚠️")
            .setContentText("Some features might not work due to unstable connection.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2001, notification)
    }

    /**
     * Copies the selected gallery image to internal storage for persistence.
     */
    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = "recipe_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
