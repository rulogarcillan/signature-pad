plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.tuppersoft.signaturepad"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            freeCompilerArgs.add("-Xexplicit-api=strict")
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
}


// ====================================
// Maven Publishing Configuration
// ====================================
mavenPublishing {
    publishToMavenCentral(true)
    signAllPublications()

    coordinates(
        groupId = "com.tuppersoft",
        artifactId = "signature-pad",
        version = project.findProperty("version") as? String
            ?: error("The 'version' parameter has not been defined. Use -Pversion=...")
    )

    pom {
        name.set("Signature Pad")
        description.set("A simple Android library for capturing signatures using a touch screen for Jetpack Compose.")
        inceptionYear.set("2025")
        url.set("https://github.com/rulogarcillan/signature-pad")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("Rulo Garcillan")
                name.set("Rulo Garcillan")
                email.set("raulrcs@gmail.com")
            }
        }

        organization {
            name.set("Tuppersoft")
            url.set("https://tuppersoft.com")
        }

        scm {
            url.set("https://github.com/rulogarcillan/signature-pad")
            connection.set("scm:git:git://github.com/rulogarcillan/signature-pad.git")
            developerConnection.set("scm:git:ssh://github.com:rulogarcillan/signature-pad.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/rulogarcillan/signature-pad/issues")
        }
    }
}
