plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform) // Changed from kotlin.android
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    androidTarget { 
        publishLibraryVariants("release")
    }
    
    jvm("desktop")
    
    // Future: iosX64(), iosArm64(), iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.androidx.compose.runtime)
                implementation(libs.androidx.compose.ui)
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.compose.ui.graphics) // Critical for rendering
                implementation(libs.kotlinx.datetime)
            }
        }
        val androidMain by getting {
            dependencies {
                // Android specific dependencies if any
                implementation(libs.androidx.core.ktx)
            }
        }
        val desktopMain by getting {
             dependencies {
                 implementation("org.jetbrains.compose.ui:ui-desktop:${libs.versions.composeMultiplatform.get()}")
                 implementation("org.jetbrains.compose.material:material-desktop:${libs.versions.composeMultiplatform.get()}")
             }
        }
    }
    
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-api=strict")
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

android {
    namespace = "com.tuppersoft.signaturepad"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }
    
    buildFeatures {
        compose = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
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
