package com.example.recipebytes.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.MediaManager
import com.example.recipebytes.BuildConfig
import com.example.recipebytes.R
import com.example.recipebytes.activities.MainActivity
import com.example.recipebytes.activities.MyRecipesActivity
import com.example.recipebytes.activities.SignInActivity
import com.example.recipebytes.preferences.UserPreferencesRepository
import com.example.recipebytes.services.FirebaseAuthService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProfileFragment : Fragment() {
    private val authService = FirebaseAuthService()
    private var selectedImageUri: Uri? = null
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Void?>
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val fg = this@ProfileFragment
            if (uri != null && fg.isAdded) {
                try {
                    val localUri = fg.copyContentUriToCache(uri)
                    if (localUri != null) {
                        selectedImageUri = localUri
                        fg.uploadProfileImage(localUri)
                    } else {
                        if (fg.isAdded) Toast.makeText(fg.requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    if (fg.isAdded) Toast.makeText(fg.requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            val fg = this@ProfileFragment
            if (bitmap != null && fg.isAdded) {
                fg.view?.findViewById<ImageView>(R.id.profileImage)?.let {
                    Glide.with(fg).load(bitmap).circleCrop().into(it)
                }
                fg.uploadBitmapDirectly(bitmap)
            }
        }

        requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val fg = this@ProfileFragment
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                if (fg.isAdded) Toast.makeText(fg.requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadBitmapDirectly(bitmap: Bitmap) {
        val ctx = context ?: return
        val cacheFile = File(ctx.cacheDir, "upload_${UUID.randomUUID()}.jpg")
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        uploadProfileImage(Uri.fromFile(cacheFile))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize preferences repository
        preferencesRepository = UserPreferencesRepository(requireContext())

        view.findViewById<TextView>(R.id.headerTitle).text = "My Profile"

        val logoutIcon = view.findViewById<ImageView>(R.id.primaryIcon)
        logoutIcon.setImageResource(android.R.drawable.ic_lock_power_off)
        logoutIcon.visibility = View.VISIBLE
        logoutIcon.setOnClickListener { showLogoutDialog() }

        view.findViewById<View>(R.id.editProfileIcon).setOnClickListener { showImagePicker() }

        loadUserProfile(view)

        view.findViewById<View>(R.id.btnUpdateProfile).setOnClickListener {
            val username = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editUsername).text.toString().trim()
            val bio = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editBio).text.toString().trim()
            val userId = authService.getCurrentUserId()
            if (userId != null) {
                val updates = mutableMapOf<String, Any>()
                if (username.isNotEmpty()) updates["username"] = username
                if (bio.isNotEmpty()) updates["bio"] = bio
                if (updates.isNotEmpty()) {
                    authService.updateUser(userId, updates,
                        onSuccess = {
                            Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(requireContext(), "Failed to update: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup theme toggle
        setupThemeToggle(view)

        view.findViewById<View>(R.id.tabMyRecipes).setOnClickListener {
            startActivity(Intent(requireContext(), MyRecipesActivity::class.java))
        }

        view.findViewById<View>(R.id.tabFavoriteRecipes).setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.putExtra("open_favorites", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun showImagePicker() {
        val items = arrayOf("🖼️ Gallery", "📷 Camera")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            }
            .show()
    }

    private fun copyContentUriToCache(uri: Uri): Uri? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val cacheFile = File(requireContext().cacheDir, "profile_gallery_${UUID.randomUUID()}.jpg")
            FileOutputStream(cacheFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupThemeToggle(view: View) {
        val themeSwitchCompat = view.findViewById<SwitchCompat?>(R.id.switchTheme)
        if (themeSwitchCompat != null) {
            // Temporarily disable listener to set initial state without triggering recreate
            themeSwitchCompat.setOnCheckedChangeListener(null)
            // Get initial state once (not continuous collect)
            lifecycleScope.launch {
                val isDarkMode = preferencesRepository.isDarkModeFlow.first()
                themeSwitchCompat.isChecked = isDarkMode
                // Re-enable listener after initial state is set
                themeSwitchCompat.setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        preferencesRepository.setDarkMode(isChecked)
                        val nightMode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                        else AppCompatDelegate.MODE_NIGHT_NO
                        AppCompatDelegate.setDefaultNightMode(nightMode)
                        activity?.recreate()
                    }
                }
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val userId = authService.getCurrentUserId() ?: return
        val ctx = context ?: return

        Toast.makeText(ctx, "Uploading...", Toast.LENGTH_SHORT).show()

        // ✅ FIX 1: Unique public_id every upload — forces Cloudinary to create a new asset
        val timestamp = System.currentTimeMillis()
        val publicId = "profile_images/${userId}_$timestamp"

        MediaManager.get().upload(uri)
            .option("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .option("public_id", publicId)
            // ✅ FIX 2: Invalidate Cloudinary's CDN cache for this resource
            .option("invalidate", true)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    Log.d("ProfileFragment", "Cloudinary upload OK, URL: $imageUrl")
                    val fg = this@ProfileFragment
                    authService.updateUser(userId, mapOf("profileImage" to imageUrl),
                        onSuccess = {
                            if (fg.isAdded) {
                                fg.view?.findViewById<ImageView>(R.id.profileImage)?.let { iv ->
                                    Glide.with(fg)
                                        .load(imageUrl)
                                        .circleCrop()
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        // ✅ FIX 3: Add cache-busting query param so Glide treats it as a new URL
                                        .load("$imageUrl?t=$timestamp")
                                        .error(R.drawable.ic_profile)
                                        .into(iv)
                                }
                            }
                            if (fg.isAdded) Toast.makeText(ctx, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            if (fg.isAdded) Toast.makeText(ctx, "Failed to save URL: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    if (isAdded) Toast.makeText(ctx, "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
    private fun showLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<View>(R.id.ivCloseDialog).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btnLogoutConfirm).setOnClickListener {
            dialog.dismiss()
            logout()
        }
        dialog.show()
    }

    private fun loadUserProfile(view: View) {
        if (!isAdded) return
        val userId = authService.getCurrentUserId()
        if (userId == null) {
            view.findViewById<TextView>(R.id.tvJoinedDate).text = ""
            return
        }

        authService.fetchUserFromDatabase(userId,
            onSuccess = { user ->
                if (!isAdded) return@fetchUserFromDatabase
                if (user != null) {
                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editUsername).setText(user.username)
                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editBio).setText(user.bio)
                    if (user.profileImage.isNotEmpty()) {
                        try {
                            // ✅ FIX 4: Always bypass Glide disk cache when loading profile image
                            Glide.with(this)
                                .load(user.profileImage)
                                .circleCrop()
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(view.findViewById(R.id.profileImage))
                        } catch (_: Exception) {}
                    }
                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    val joinedDate = dateFormat.format(Date(user.createdAt))
                    view.findViewById<TextView>(R.id.tvJoinedDate).text = "Joined $joinedDate"
                }
            },
            onError = {
                if (isAdded) view.findViewById<TextView>(R.id.tvJoinedDate).text = ""
            }
        )
    }

    private fun logout() {
        val prefs = requireContext().getSharedPreferences("login_prefs", 0)
        prefs.edit().clear().apply()
        authService.signOut()
        
        // Clear saved preferences on logout
        lifecycleScope.launch {
            preferencesRepository.clearPreferences()
        }
        
        Toast.makeText(requireContext(), "Logged out!", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
