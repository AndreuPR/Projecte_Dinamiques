plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.dinamiqapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dynamicsapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-beta"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // El nom que tindrà l'app oficial instal·lada
            manifestPlaceholders["appName"] = "DinamiqApp"
        }
        debug {
            // Això farà que l'ID sigui "com.dynamicsapp.dev"
            applicationIdSuffix = ".dev"
            // El nom que tindrà aquesta versió de proves al mòbil
            manifestPlaceholders["appName"] = "DinamiqApp DEV"
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