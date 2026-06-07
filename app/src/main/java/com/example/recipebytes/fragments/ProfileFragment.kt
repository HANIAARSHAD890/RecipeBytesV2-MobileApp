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
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.recipebytes.R
import com.example.recipebytes.activities.MainActivity
import com.example.recipebytes.activities.MyRecipesActivity
import com.example.recipebytes.activities.SignInActivity
import com.example.recipebytes.preferences.UserPreferencesRepository
import com.example.recipebytes.services.FirebaseAuthService
import com.google.firebase.storage.FirebaseStorage
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
    private val storageRef = FirebaseStorage.getInstance().reference
    private var selectedImageUri: Uri? = null
    private lateinit var preferencesRepository: UserPreferencesRepository

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val localUri = copyContentUriToCache(uri)
                if (localUri != null) {
                    selectedImageUri = localUri
                    view?.findViewById<ImageView>(R.id.profileImage)?.let {
                        Glide.with(this).load(uri).circleCrop().into(it)
                    }
                    uploadProfileImage(localUri)
                } else {
                    Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            try {
                val cacheFile = File(requireContext().cacheDir, "profile_camera_${UUID.randomUUID()}.jpg")
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val uri = Uri.fromFile(cacheFile)
                selectedImageUri = uri
                view?.findViewById<ImageView>(R.id.profileImage)?.let {
                    Glide.with(this).load(bitmap).circleCrop().into(it)
                }
                uploadProfileImage(uri)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to process camera image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
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
                    1 -> requestCameraPermission.launch(android.Manifest.permission.CAMERA)
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
        val ref = storageRef.child("profile_images/$userId/profile.jpg")
        
        try {
            ref.putFile(uri)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                            val url = downloadUrl.toString()
                            authService.updateUser(userId, mapOf("profileImage" to url),
                                onSuccess = {
                                    Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    Toast.makeText(requireContext(), "Failed to save: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        android.util.Log.e("ProfileFragment", "Upload error", e)
                    }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("ProfileFragment", "Image processing error", e)
        }
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
        val userId = authService.getCurrentUserId()
        if (userId == null) {
            view.findViewById<TextView>(R.id.tvJoinedDate).text = ""
            return
        }

        authService.fetchUserFromDatabase(userId,
            onSuccess = { user ->
                if (user != null) {
                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editUsername).setText(user.username)
                    view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editBio).setText(user.bio)

                    if (user.profileImage.isNotEmpty()) {
                        Glide.with(this)
                            .load(user.profileImage)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(view.findViewById(R.id.profileImage))
                    }

                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    val joinedDate = dateFormat.format(Date(user.createdAt))
                    view.findViewById<TextView>(R.id.tvJoinedDate).text = "Joined $joinedDate"
                }
            },
            onError = {
                view.findViewById<TextView>(R.id.tvJoinedDate).text = ""
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
