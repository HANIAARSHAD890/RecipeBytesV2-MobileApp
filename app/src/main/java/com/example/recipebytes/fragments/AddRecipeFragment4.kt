package com.example.recipebytes.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.recipebytes.R
import com.example.recipebytes.activities.AddRecipeActivity
import com.example.recipebytes.models.Ingredient
import com.example.recipebytes.models.Nutrition
import com.example.recipebytes.models.Recipe
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.models.Step
import com.example.recipebytes.services.FirebaseAuthService
import com.example.recipebytes.services.FirebaseRecipeService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.io.FileOutputStream

class AddRecipeFragment4 : Fragment(R.layout.activity_add_recipe_fragment4) {

    private var imageUri2: Uri?    = null
    private var finalImagePath: String? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                imageUri2      = it
                finalImagePath = saveImageToInternalStorage(it)
                view?.let { v -> showImagePreview(v, it.toString()) }
            }
        }

        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                cameraImageUri?.let {
                    imageUri2      = it
                    finalImagePath = it.toString()
                    view?.let { v -> showImagePreview(v, it.toString()) }
                }
            }
        }

        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(),
                "Camera permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePreview  = view.findViewById<ImageView>(R.id.imagePreview)
        val btnRemove     = view.findViewById<ImageButton>(R.id.btnRemoveImage)
        val btnGallery    = view.findViewById<LinearLayout>(R.id.btnPickImage)
        val btnCamera     = view.findViewById<LinearLayout>(R.id.btnCamera)
        val btnUrlOption  = view.findViewById<LinearLayout>(R.id.btnUrlOption)
        val tilUrl        = view.findViewById<TextInputLayout>(R.id.tilImageUrl)
        val urlInput      = view.findViewById<TextInputEditText>(R.id.editImageUrl)
        val btnLoadUrl    = view.findViewById<MaterialButton>(R.id.btnLoadUrl)
        val tvError       = view.findViewById<TextView>(R.id.tvImageError)
        val btnSave       = view.findViewById<MaterialButton>(R.id.btnSave)
        val loader        = view.findViewById<ProgressBar>(R.id.loader)

        btnGallery.setOnClickListener {
            hideUrlInput(tilUrl, btnLoadUrl)
            pickImageLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            hideUrlInput(tilUrl, btnLoadUrl)
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnUrlOption.setOnClickListener {
            tilUrl.visibility     = View.VISIBLE
            btnLoadUrl.visibility = View.VISIBLE
            urlInput.requestFocus()
        }

        btnLoadUrl.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isEmpty()) {
                tilUrl.error = "Please enter a URL"
                return@setOnClickListener
            }
            if (!isInternetAvailable()) {
                tilUrl.error = "No internet connection"
                return@setOnClickListener
            }
            loadImageFromUrl(url, imagePreview, loader, tilUrl, tvError, btnRemove)
        }

        urlInput.addTextChangedListener {
            tilUrl.error = null
            tvError.visibility = View.GONE
        }

        btnRemove.setOnClickListener {
            imageUri2      = null
            finalImagePath = null
            imagePreview.setImageResource(R.drawable.camera_upload_icon)
            btnRemove.visibility = View.GONE
            urlInput.setText("")
            tvError.visibility = View.GONE
            Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            handleSave(tvError)
        }

        if (!isInternetAvailable()) {
            showNoInternetNotification()
        }
    }

    private fun openCamera() {
        val photoFile = File(requireContext().filesDir,
            "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        takePictureLauncher.launch(cameraImageUri!!)
    }

    private fun loadImageFromUrl(
        url: String,
        imagePreview: ImageView,
        loader: ProgressBar,
        tilUrl: TextInputLayout,
        tvError: TextView,
        btnRemove: ImageButton
    ) {
        loader.visibility = View.VISIBLE
        Glide.with(this)
            .load(url)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    loader.visibility  = View.GONE
                    tilUrl.error       = "Could not load image from URL"
                    tvError.visibility = View.VISIBLE
                    return false
                }
                override fun onResourceReady(
                    resource: Drawable, model: Any,
                    target: Target<Drawable>?, dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loader.visibility    = View.GONE
                    tilUrl.error         = null
                    tvError.visibility   = View.GONE
                    finalImagePath       = url
                    btnRemove.visibility = View.VISIBLE
                    return false
                }
            })
            .into(imagePreview)
    }

    private fun showImagePreview(view: View, uriString: String) {
        val imagePreview = view.findViewById<ImageView>(R.id.imagePreview)
        val btnRemove    = view.findViewById<ImageButton>(R.id.btnRemoveImage)
        Glide.with(this).load(uriString).centerCrop().into(imagePreview)
        btnRemove.visibility = View.VISIBLE
    }

    private fun hideUrlInput(tilUrl: TextInputLayout, btnLoadUrl: MaterialButton) {
        tilUrl.visibility     = View.GONE
        btnLoadUrl.visibility = View.GONE
    }

    private fun handleSave(tvError: TextView) {
        if (imageUri2 == null) {
            tvError.text       = "⚠️ Please select an image first"
            tvError.visibility = View.VISIBLE
            return
        }

        val title       = arguments?.getString("title")       ?: ""
        val desc        = arguments?.getString("desc")        ?: ""
        val category    = arguments?.getString("category")    ?: ""
        val cookingTime = arguments?.getString("cookingTime")
            ?.filter { it.isDigit() }
            ?.toIntOrNull() ?: 0

        val ingredients = arguments?.getSerializable("ingredients")
                as? ArrayList<Ingredient> ?: arrayListOf()
        val steps       = arguments?.getSerializable("steps")
                as? ArrayList<Step> ?: arrayListOf()

        // ✅ FIXED: nutrition now passed from arguments
        val nutrition   = arguments?.getSerializable("nutrition") as? Nutrition

        val recipe = Recipe(
            title       = title,
            description = desc,
            category    = category,
            imageUri    = "",  // Will be updated with download URL
            ingredients = ingredients,
            steps       = steps,
            cookingTime = cookingTime,
            nutrition   = nutrition    // ✅ FIXED: was missing before
        )

        RecipeRepository.addRecipe(requireContext(), recipe)
        (activity as? AddRecipeActivity)?.onRecipeSaved()

        Toast.makeText(requireContext(),
            "Recipe saved successfully! 🎉", Toast.LENGTH_SHORT).show()
        requireActivity().finish()
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream  = requireContext().contentResolver.openInputStream(uri)
            val fileName     = "recipe_${System.currentTimeMillis()}.jpg"
            val file         = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { it.copyTo(outputStream) }
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = requireContext().getSystemService(
            android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
        val network      = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(
            android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoInternetNotification() {
        val channelId = "internet_alert_channel"
        val nm = requireContext().getSystemService(
            android.content.Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                android.app.NotificationChannel(
                    channelId, "Internet Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val n = androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("No Internet Connection ⚠️")
            .setContentText("Image URLs won't load. Use Gallery or Camera instead.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(2001, n)
    }
}