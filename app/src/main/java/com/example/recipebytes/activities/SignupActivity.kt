package com.example.recipebytes.activities

import android.content.Intent
import android.os.Bundle
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
    }

    private fun handleSignUp() {
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

        showProgress(true)

        // Call Firebase service
        authService.signUp(email, password,
            onSuccess = { userId ->
                showProgress(false)
                Toast.makeText(this, "Account created successfully! Please sign in.", Toast.LENGTH_SHORT).show()
                authService.signOut()
                navigateToSignIn()
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

    private fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
//
//    private fun navigateToHome(userId: String) {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.putExtra("userId", userId)
//        startActivity(intent)
//        finish()
   // }
}