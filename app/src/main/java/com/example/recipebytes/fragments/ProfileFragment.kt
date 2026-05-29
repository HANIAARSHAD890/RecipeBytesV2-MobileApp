package com.example.recipebytes.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.recipebytes.R
import com.example.recipebytes.activities.MainActivity
import com.example.recipebytes.activities.MyRecipesActivity
import com.example.recipebytes.activities.SignInActivity
import com.example.recipebytes.services.FirebaseAuthService
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProfileFragment : Fragment() {
    private val authService = FirebaseAuthService()
    private val storageRef = FirebaseStorage.getInstance().reference
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            view?.findViewById<ImageView>(R.id.profileImage)?.let {
                Glide.with(this).load(uri).circleCrop().into(it)
            }
            uploadProfileImage(uri)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = Uri.parse(
                MediaStore.Images.Media.insertImage(
                    requireContext().contentResolver, bitmap, "Profile_${UUID.randomUUID()}", null
                )
            )
            selectedImageUri = uri
            view?.findViewById<ImageView>(R.id.profileImage)?.let {
                Glide.with(this).load(bitmap).circleCrop().into(it)
            }
            uploadProfileImage(uri)
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

        view.findViewById<TextView>(R.id.headerTitle).text = "My Profile"

        val logoutIcon = view.findViewById<ImageView>(R.id.primaryIcon)
        logoutIcon.setImageResource(android.R.drawable.ic_lock_power_off)
        logoutIcon.visibility = View.VISIBLE
        logoutIcon.setOnClickListener { showLogoutDialog() }

        view.findViewById<View>(R.id.editProfileIcon).setOnClickListener { showImagePicker() }

        loadUserProfile(view)

        view.findViewById<View>(R.id.btnUpdateProfile).setOnClickListener {
            val username = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editUsername).text.toString()
            val bio = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editBio).text.toString()
            Toast.makeText(requireContext(), "Profile updated! Username: $username", Toast.LENGTH_SHORT).show()
        }

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
                    1 -> cameraLauncher.launch(null)
                }
            }
            .show()
    }

    private fun uploadProfileImage(uri: Uri) {
        val userId = authService.getCurrentUserId() ?: return
        val ref = storageRef.child("profile_images/$userId/profile.jpg")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val url = downloadUrl.toString()
                    authService.updateUser(userId, mapOf("profileImage" to url),
                        onSuccess = {
                            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(requireContext(), "Logged out!", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
