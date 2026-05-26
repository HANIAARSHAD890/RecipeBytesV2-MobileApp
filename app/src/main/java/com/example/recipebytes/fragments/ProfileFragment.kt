package com.example.recipebytes.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.recipebytes.R
import com.example.recipebytes.activities.SignInActivity
import com.example.recipebytes.services.FirebaseAuthService

class ProfileFragment : Fragment() {
    private val authService = FirebaseAuthService()

    private lateinit var uidText: TextView
    private lateinit var emailText: TextView
    private lateinit var logoutBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uidText = view.findViewById(R.id.userIdText)
        emailText = view.findViewById(R.id.userEmailText)
        logoutBtn = view.findViewById(R.id.logoutBtn)

        loadUserData()

        logoutBtn.setOnClickListener {
            logout()
        }
    }

    private fun loadUserData() {
        val userId = authService.getCurrentUserId()

        if (userId == null) {
            uidText.text = "UID: N/A"
            emailText.text = "Email: N/A"
            return
        }

        authService.fetchUserFromDatabase(
            userId,
            onSuccess = { user ->
                if (user != null) {
                    uidText.text = "UID: ${user.uid}"
                    emailText.text = "Email: ${user.email}"
                } else {
                    uidText.text = "UID: $userId"
                    emailText.text = "Email: ${authService.getCurrentUserEmail() ?: "N/A"}"
                }
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load user data: $error",
                    Toast.LENGTH_SHORT
                ).show()

                uidText.text = "UID: $userId"
                emailText.text = "Email: ${authService.getCurrentUserEmail() ?: "N/A"}"
            }
        )
    }

    private fun logout() {
        authService.signOut()
        requireContext().getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        Toast.makeText(requireContext(), "Logged out!", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        activity?.finish()
    }
}