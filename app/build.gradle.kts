plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidTarget()
    
    jvm("desktop")
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.androidx.compose.runtime)
                implementation(libs.androidx.compose.ui)
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.compose.material3)
                implementation(libs.androidx.compose.material.icons.extended)
                implementation(projects.signaturePad)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime-desktop:1.7.1")
                implementation("org.jetbrains.compose.ui:ui-desktop:1.7.1")
                implementation("org.jetbrains.compose.foundation:foundation-desktop:1.7.1")
                implementation("org.jetbrains.compose.material3:material3-desktop:1.7.1")
                implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.7.1")
            }
        }
    }
}

android {
    namespace = "com.tuppersoft.signaturepad.sample"
    compileSdk = 36

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
    buildFeatures {
        compose = true
    }
}
