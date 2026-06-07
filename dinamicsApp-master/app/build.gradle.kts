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

    flavorDimensions += "versio"

    // 2. Creem els diferents "sabors" (versions) que vols tenir al mòbil a la vegada
    productFlavors {
        create("estable") {
            dimension = "versio"
            // Aquesta serà l'app original. No li posem sufix.
            manifestPlaceholders["appName"] = "DinamiqApp Original"
        }
        create("v3") {
            dimension = "versio"
            applicationIdSuffix = ".v3" // ID: com.dynamicsapp.v3
            manifestPlaceholders["appName"] = "DinamiqApp v3"
        }
        create("v4") {
            dimension = "versio"
            applicationIdSuffix = ".v4" // ID: com.dynamicsapp.v4
            manifestPlaceholders["appName"] = "DinamiqApp v4"
        }
        create("v5") {
            dimension = "versio"
            applicationIdSuffix = ".v5" // Clau: farà que l'ID sigui com.dynamicsapp.v5
            manifestPlaceholders["appName"] = "DinamiqApp v5" // El nom al mòbil
        }
        create("v6") {
            dimension = "versio"
            applicationIdSuffix = ".v6" // Clau: farà que l'ID sigui com.dynamicsapp.v5
            manifestPlaceholders["appName"] = "DinamiqApp v6" // El nom al mòbil
        }
        create("v7") {
            dimension = "versio"
            applicationIdSuffix = ".v7" // Clau: farà que l'ID sigui com.dynamicsapp.v5
            manifestPlaceholders["appName"] = "Voice_Rec_DinamiqApp" // El nom al mòbil
        }
        create("v8") {
            dimension = "versio"
            applicationIdSuffix = ".v8" // Clau: farà que l'ID sigui com.dynamicsapp.v5
            manifestPlaceholders["appName"] = "Voice_Rec_DinamiqApp2" // El nom al mòbil
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            // Eliminem el sufix d'aquí perquè ja el controlen els flavors de dalt
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

}