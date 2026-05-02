plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.recipebytes"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.recipebytes"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    //for image caching
    implementation("com.github.bumptech.glide:glide:4.16.0")
    //for the image
    implementation("com.squareup.picasso:picasso:2.8")
    //for the material UI
    implementation("com.google.android.material:material:1.9.0")
    // for storing data
    implementation ("com.google.code.gson:gson:2.10.1")
    // for navigation via cards in fragments
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}