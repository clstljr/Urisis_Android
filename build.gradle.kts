// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // KSP plugin declared at project level so all modules can use it
    // Version 2.0.21-1.0.27 matches the Kotlin version used in this project
    // "apply false" means it's declared here but only activated in app/build.gradle.kts
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}