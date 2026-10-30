import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.androidx.room3.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    detektPlugins(libs.compose.rules.detekt)
    testImplementation(libs.kotlin.test)
}

// This module builds `repz.quality`, so it cannot apply it; ktlint and Detekt are configured here
// by hand, with the same `.editorconfig` and `config/detekt/detekt.yml` as every other module. A
// local function, not top-level script code: a lambda capturing a top-level script `val` cannot be
// serialized for the configuration cache, but one capturing a function-local `val` can.
fun Project.configureQuality() {
    val moduleBuildDirectory =
        layout.buildDirectory
            .get()
            .asFile
            .toPath()
    val moduleSourceDirectories =
        layout.projectDirectory
            .dir("src")
            .asFile
            .listFiles { file -> file.isDirectory }
            ?.map { sourceSetDir -> sourceSetDir.resolve("kotlin") }
            ?.filter { it.exists() }
            .orEmpty()

    extensions.configure<KtlintExtension> {
        version.set(libs.versions.ktlint.asProvider())
        ignoreFailures.set(false)
        filter {
            // Compared by path, not a "build" substring: a checkout path containing "build"
            // anywhere above this module must not match.
            exclude { entry -> entry.file.toPath().startsWith(moduleBuildDirectory) }
        }
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        allRules.set(false)
        parallel.set(true)
        ignoreFailures.set(false)
        autoCorrect.set(false)
        disableDefaultRuleSets.set(false)
        config.setFrom(
            rootProject.layout.projectDirectory
                .dir("..")
                .file("config/detekt/detekt.yml"),
        )
        basePath.set(rootProject.layout.projectDirectory.dir(".."))
        // Every src/<sourceSet>/kotlin directory, not just src/main/kotlin, so a future
        // src/test/kotlin here is covered too (same discovery as repz.quality's kotlinSourceDirectories()).
        source.setFrom(moduleSourceDirectories)
    }
}

configureQuality()

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "repz.kmp.library"
            implementationClass = "com.repz.buildlogic.KmpLibraryConventionPlugin"
        }
        register("room") {
            id = "repz.room"
            implementationClass = "com.repz.buildlogic.RoomConventionPlugin"
        }
        register("compose") {
            id = "repz.compose"
            implementationClass = "com.repz.buildlogic.ComposeConventionPlugin"
        }
        register("koin") {
            id = "repz.koin"
            implementationClass = "com.repz.buildlogic.KoinConventionPlugin"
        }
        register("kmpFeatureApi") {
            id = "repz.kmp.feature.api"
            implementationClass = "com.repz.buildlogic.KmpFeatureApiConventionPlugin"
        }
        register("kmpFeatureImpl") {
            id = "repz.kmp.feature.impl"
            implementationClass = "com.repz.buildlogic.KmpFeatureImplConventionPlugin"
        }
        register("androidApplication") {
            id = "repz.android.application"
            implementationClass = "com.repz.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("quality") {
            id = "repz.quality"
            implementationClass = "com.repz.buildlogic.QualityConventionPlugin"
        }
    }
}
