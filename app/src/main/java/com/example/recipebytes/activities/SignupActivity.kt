package com.example.recipebytes.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

// Handles new user registration with email/password validation
class SignUpActivity : AppCompatActivity() {

    private val authService = FirebaseAuthService()
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var signUpBtn: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var signInLink: TextView

    companion object {
        private const val TAG = "SignUpActivity"
    }

    // Initializes views and sets up validation and button listeners on creation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize views
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        signUpBtn = findViewById(R.id.signUpBtn)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        signInLink = findViewById(R.id.signInLink)

        // Set up listeners
        signUpBtn.setOnClickListener { handleSignUp() }
        signInLink.setOnClickListener { navigateToSignIn() }
        
        // Add real-time validation
        setupEmailValidation()
        setupPasswordValidation()
    }

    // Adds real-time email format validation as the user types
    private fun setupEmailValidation() {
        emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                when {
                    email.isEmpty() -> {
                        emailLayout.error = "Email is required"
                    }
                    !isValidEmail(email) -> {
                        emailLayout.error = "Please enter a valid email address"
                    }
                    else -> {
                        emailLayout.error = null
                    }
                }
            }
        })
    }

    // Adds real-time password strength validation as the user types
    private fun setupPasswordValidation() {
        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val errors = mutableListOf<String>()

                if (password.isEmpty()) {
                    errors.add("Password is required")
                } else {
                    if (password.length < 8) {
                        errors.add("• Minimum 8 characters")
                    }
                    if (!password.any { it.isUpperCase() }) {
                        errors.add("• At least one uppercase letter")
                    }
                    if (!password.any { it.isLowerCase() }) {
                        errors.add("• At least one lowercase letter")
                    }
                    if (!password.any { it.isDigit() }) {
                        errors.add("• At least one number")
                    }
                }

                if (errors.isNotEmpty()) {
                    passwordLayout.error = errors.joinToString("\n")
                } else {
                    passwordLayout.error = null
                }
            }
        })
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length > 5
    }

    // Validates input and calls Firebase auth to create a new account
    private fun handleSignUp() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Clear previous errors
        errorText.visibility = View.GONE

        if (email.isEmpty()) {
            showError("Please enter email")
            emailInput.requestFocus()
            return
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address")
            emailInput.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showError("Please enter password")
            passwordInput.requestFocus()
            return
        }

        if (password.length < 8) {
            showError("Password must be at least 8 characters")
            passwordInput.requestFocus()
            return
        }

        showProgress(true)

        // Call Firebase service
        authService.signUp(email, password,
            onSuccess = { userId ->
                showProgress(false)
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                
                // Mark onboarding as not done for this new user
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("onboarding_done_$userId", false)
                    .apply()
                
                // Navigate to onboarding directly
                val intent = Intent(this, OnboardingActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
                finish()
            },
            onError = { error ->
                showProgress(false)
                showError(error)
            }
        )
    }

    private fun showError(message: String) {
        errorText.text = "$message"
        errorText.visibility = View.VISIBLE
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        signUpBtn.isEnabled = !show
    }

    // Navigates to the sign-in activity
    private fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
}
