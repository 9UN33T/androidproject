plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.taskmanager"
    compileSdk = 34  // Changed from 35 to 34 as it's the latest stable version

    defaultConfig {
        applicationId = "com.example.taskmanager"
        minSdk = 30
        targetSdk = 34  // Changed from 35 to 34 to match compileSdk
        versionCode = 1
        versionName = "1.0"
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
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
}