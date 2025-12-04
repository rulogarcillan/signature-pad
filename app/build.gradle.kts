plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tuppersoft.signaturepad.sample"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.tuppersoft.signaturepad.sample"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Activity Compose
    // Proporciona: ComponentActivity, setContent, enableEdgeToEdge
    implementation(libs.androidx.activity.compose)

    // Compose BOM - gestiona automáticamente las versiones
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI
    // Proporciona: Modifier, Column, Row, Spacer, fillMaxSize, etc
    implementation(libs.androidx.compose.ui)

    // Compose Material3
    // Proporciona: MaterialTheme, Button, Text, Icon, Scaffold, Surface, colorScheme, etc
    implementation(libs.androidx.compose.material3)

    // Material Icons Extended
    // Proporciona: Icons.AutoMirrored.Filled.Undo, Icons.AutoMirrored.Filled.Redo, Icons.Filled.Clear
    implementation(libs.androidx.compose.material.icons.extended)

    // Módulo de la librería SignaturePad
    implementation(projects.signaturePad)
}
