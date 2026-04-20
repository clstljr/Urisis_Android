plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // KSP (Kotlin Symbol Processing) — modern replacement for kapt
    // Required to generate Room database code during compilation
    // kapt was removed because it's incompatible with built-in Kotlin support
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.urisis_android"

    // compileSdk — the Android API version used to compile the app
    // Set to 35 (Android 15) — stable version, 36 was causing build errors
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.urisis_android"

        // minSdk — the oldest Android version the app can run on
        // Set to 24 (Android 7.0) to fix INSTALL_FAILED_OLDER_SDK error
        // Covers 99% of Android devices in use today
        minSdk = 24

        // targetSdk — the Android version the app is optimized for
        // Set to 35 to match compileSdk, 36 was unstable/preview
        targetSdk = 35
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    // ── Room Database ────────────────────────────────────────────────────────
    // Room — Android's official local database library (SQLite wrapper)
    // Used to store USER and TEST_RESULT tables locally on the device
    // Matches the ERD designed in the thesis
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")  // core Room library
    implementation("androidx.room:room-ktx:$room_version")  // Kotlin extensions for Room (suspend functions)
    ksp("androidx.room:room-compiler:$room_version")  // generates Room code at compile time (was kapt, changed to ksp)

    // ── Coroutines ───────────────────────────────────────────────────────────
    // Kotlin Coroutines — allows running database operations in background
    // Required because Room suspend functions run on background threads
    // Prevents app from freezing while reading/writing to database
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ── ViewModel ────────────────────────────────────────────────────────────
    // Lifecycle ViewModel for Compose — connects UI screens to business logic
    // Used by AuthViewModel to handle login and register operations
    // viewModel() function in LoginScreen and RegisterScreen requires this
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}