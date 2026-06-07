plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.cridar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cridar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-beta"
    }

    flavorDimensions += "versio"

    // 2. Creem els diferents "sabors" (versions) que vols tenir al mòbil a la vegada
    productFlavors {
        create("estable") {
            dimension = "versio"
            // Aquesta serà l'app original. No li posem sufix.
            manifestPlaceholders["appName"] = "No Cridis Original"
        }
        create("v3") {
            dimension = "versio"
            applicationIdSuffix = ".v1" // ID: com.dynamicsapp.v1
            manifestPlaceholders["appName"] = "No Cridis  v3"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
}