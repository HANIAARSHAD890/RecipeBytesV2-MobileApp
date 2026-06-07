package com.example.recipebytes

import android.app.Application
import com.cloudinary.android.MediaManager
import com.example.recipebytes.BuildConfig
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key"    to BuildConfig.CLOUDINARY_API_KEY
        )
        MediaManager.init(this, config)
    }
}