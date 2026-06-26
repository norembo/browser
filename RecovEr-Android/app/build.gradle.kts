plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace  = "com.recover.app"
    compileSdk = 35

    defaultConfig {
        applicationId   = "com.recover.app"
        minSdk          = 26
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0"

        // Replace with your real Stripe publishable key
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"pk_test_YOUR_KEY_HERE\"")
        // Replace with your backend base URL
        buildConfigField("String", "BACKEND_URL", "\"https://api.recover-app.com\"")
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Health Connect (Google Fit replacement)
    implementation(libs.androidx.health.connect)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Face Detection
    implementation(libs.mlkit.face.detection)

    // Stripe
    implementation(libs.stripe.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // OkHttp (Stripe backend calls)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Permissions
    implementation(libs.accompanist.permissions)

    debugImplementation(libs.androidx.ui.tooling)
}
