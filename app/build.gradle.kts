import java.util.Properties
val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { localProps.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
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

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProps["CLOUDINARY_CLOUD_NAME"]}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${localProps["CLOUDINARY_API_KEY"]}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProps["CLOUDINARY_UPLOAD_PRESET"]}\"")

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
    buildFeatures {
        buildConfig = true
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
    implementation(libs.firebase.database)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation(libs.play.services.cast.tv)
    testImplementation(libs.junit)
    //for image caching
    implementation("com.github.bumptech.glide:glide:4.16.0")
    //for the image
    implementation("com.squareup.picasso:picasso:2.8")
    //for the material UI
    implementation("com.google.android.material:material:1.9.0")
    // for storing data
    implementation ("com.google.code.gson:gson:2.10.1")
    // for DataStore preferences (state persistence)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // for navigation via cards in fragments
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    // for circular image view
    implementation("de.hdodenhof:circleimageview:3.1.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.cloudinary:cloudinary-android:2.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.tbuonomo:dotsindicator:5.0")
}