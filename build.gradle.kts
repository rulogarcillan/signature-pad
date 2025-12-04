// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
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

        // Excluir archivos generados y de prueba
        source.setFrom(
            "src/main/java",
            "src/main/kotlin"
        )

        parallel = true
        ignoreFailures = false
    }

    dependencies {
        detektPlugins(rootProject.libs.detektFormatting)
        detektPlugins(rootProject.libs.detektComposeRules)
    }
}




