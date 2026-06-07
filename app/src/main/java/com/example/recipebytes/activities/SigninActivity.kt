package com.example.recipebytes.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recipebytes.R
import com.example.recipebytes.services.FirebaseAuthService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

// Handles existing user login with email and password
class SignInActivity : AppCompatActivity() {

    private val authService = FirebaseAuthService()
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var signInBtn: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var signUpLink: TextView

    companion object {
        private const val TAG = "SignInActivity"
    }

    // Initializes views and sets up button listeners on creation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        // Initialize views
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        signInBtn = findViewById(R.id.signInBtn)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        signUpLink = findViewById(R.id.signUpLink)

        // Set up listeners
        signInBtn.setOnClickListener { handleSignIn() }
        signUpLink.setOnClickListener { navigateToSignUp() }
    }

    // Validates input and calls Firebase auth to sign in the user
    private fun handleSignIn() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Clear previous errors
        errorText.visibility = View.GONE

        // Validate
        if (email.isEmpty()) {
            showError("Please enter email")
            emailLayout.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showError("Please enter password")
            passwordLayout.requestFocus()
            return
        }

        Log.d(TAG, "🔐 Starting sign in process...")
        showProgress(true)

        // Call Firebase service
        authService.signIn(email, password,
            onSuccess = { userId, user ->
                Log.d(TAG, "✅ Sign in successful! UID: $userId, Email: ${user?.email}")
                showProgress(false)
                Toast.makeText(this, "Welcome back, ${user?.email}!", Toast.LENGTH_SHORT).show()
                val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("userId", userId)
                    .putString("email", user?.email ?: email)
                    .apply()
                navigateToHome(userId)
            },
            onError = { error ->
                Log.e(TAG, "❌ Sign in error: $error")
                showProgress(false)
                showError(error)
            }
        )
    }

    private fun showError(message: String) {
        errorText.text = "⚠️ $message"
        errorText.visibility = View.VISIBLE
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        signInBtn.isEnabled = !show
    }

    // Navigates to the sign-up activity
    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
        finish()
    }

    // Navigates to the main activity with the signed-in user's ID
    private fun navigateToHome(userId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
        finish()
    }
}