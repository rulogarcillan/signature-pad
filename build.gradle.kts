// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.detekt)
}

// ====================================
// Detekt Configuration for all modules
// ====================================
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        buildUponDefaultConfig = true
        autoCorrect = true
        allRules = false
        config.setFrom(files("$rootDir/detekt.yml"))

        // Source directories for KMP projects
        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/desktopMain/kotlin"
        )

        parallel = true
        ignoreFailures = false
    }

    dependencies {
        detektPlugins(rootProject.libs.detekt.formatting)
        detektPlugins(rootProject.libs.detekt.compose.rules)
    }
}




