package com.example.recipebytes.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recipebytes.R
import com.example.recipebytes.models.MealRepository
import com.example.recipebytes.models.RecipeRepository
import com.example.recipebytes.services.FirebaseAuthService

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RecipeRepository.init(applicationContext)
        MealRepository.init(applicationContext)
        val authService = FirebaseAuthService()
        setContentView(R.layout.activity_splash_screen)
        val currentUserId = authService.getCurrentUserId()
        Log.d("AUTH_TEST", "USER = $currentUserId")
        if (currentUserId != null) {

            // User already logged in
            startActivity(Intent(this, MainActivity::class.java))
            finish()

        } else {

            // User not logged in
            val btn = findViewById<Button>(R.id.btnExplore)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            btn.setOnClickListener {
                startActivity(Intent(this, SignUpActivity::class.java))
                finish()
            }
        }
    }
}
