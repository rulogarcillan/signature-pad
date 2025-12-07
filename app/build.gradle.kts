plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget()

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                // Signature Pad library
                implementation(projects.signaturePad)

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)
            }
        }

        androidMain {
            dependencies {
                // Android-specific dependencies
                implementation(compose.uiTooling)
                implementation(compose.preview)
                implementation(libs.activity.compose)
                implementation(libs.core.ktx)
            }
        }

        val desktopMain by getting {
            dependencies {
                // Desktop-specific dependencies
                implementation(compose.desktop.currentOs)
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

// Configure desktop run task
tasks.register<JavaExec>("desktopRun") {
    group = "application"
    mainClass.set("com.tuppersoft.signaturepad.sample.desktop.MainKt")
    kotlin.targets.getByName<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("desktop").compilations.getByName(
        "main"
    ).let { compilation ->
        dependsOn(compilation.compileAllTaskName)
        classpath = compilation.output.allOutputs + compilation.compileDependencyFiles
    }
}

