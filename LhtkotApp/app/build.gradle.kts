// --- Plugin Definitions ---
plugins {
    alias(libs.plugins.android.application) // Android application plugin
    alias(libs.plugins.kotlin.android)      // Kotlin Android plugin
    alias(libs.plugins.kotlin.compose)      // Jetpack Compose support for Kotlin
}

android {
    namespace = "com.example.lhtkotapp" // Package name (must match your app namespace)
    compileSdk = 35                     // Compile SDK version

    defaultConfig {
        applicationId = "com.example.lhtkotapp" // Application ID
        minSdk = 28                              // Minimum supported SDK version
        targetSdk = 35                           // Target SDK version
        versionCode = 1                          // Internal version code
        versionName = "1.0"                      // User-facing version name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // For UI testing
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Disable code shrinking for release build
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Set Java compatibility options
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Kotlin JVM target compatibility
    kotlinOptions {
        jvmTarget = "11"
    }

    // Enable Jetpack Compose
    buildFeatures {
        compose = true
    }
}

// --- Project Dependencies ---
dependencies {
    // Core AndroidX & Compose Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // MQTT Client Libraries
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5") // Core MQTT client
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1") // Android MQTT service

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging Tools for Compose
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
