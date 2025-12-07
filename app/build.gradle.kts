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
                implementation(projects.signaturePad)
            }
        }
        val androidMain by getting {
            dependencies {
                // TODO
            }
        }
        val desktopMain by getting {
            dependencies {
                // TODO
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

