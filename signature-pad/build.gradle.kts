import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }


    sourceSets {
        commonMain {
            dependencies {
                // Compose Multiplatform dependencies
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                // KotlinX DateTime
                implementation(libs.kotlinx.datetime)
            }
        }

        androidMain {
            dependencies {
                // Android-specific dependencies if needed
            }
        }

        val desktopMain by getting {
            dependencies {
                // Desktop-specific dependencies if needed
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
        artifactId = "signature-pad",  // Maven Central auto-generates: signature-pad-android, signature-pad-jvm, etc.
        version = project.findProperty("version") as? String
            ?: error("The 'version' parameter has not been defined. Use -Pversion=...")
    )

    pom {
        name.set("Signature Pad KMP")
        description.set("A Kotlin Multiplatform library for capturing smooth signatures with Bézier curves on Android, Desktop, iOS and Web using Compose Multiplatform.")
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
                id.set("rulogarcillan")
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
            developerConnection.set("scm:git:ssh://git@github.com:rulogarcillan/signature-pad.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/rulogarcillan/signature-pad/issues")
        }
    }
}
