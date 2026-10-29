package com.repz.buildlogic.quality

import com.repz.buildlogic.libs
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File

/**
 * ktlint reporters: plain for the console, checkstyle for machine consumption later in CI. KSP
 * and the Compose resources plugin register generated Kotlin under this module's own build
 * directory, which ktlint's auto-discovery then picks up; excluded since it is never hand-written.
 */
internal fun Project.configureKtlint() {
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
    // Captured before the lambda (isolated-projects-safe) and compared by path, not by a "build"
    // substring: a checkout path containing "build" anywhere above this module must not match.
    val moduleBuildDirectory =
        layout.buildDirectory
            .get()
            .asFile
            .toPath()

    extensions.configure<KtlintExtension> {
        version.set(ktlintVersion)
        ignoreFailures.set(false)
        filter {
            exclude { entry -> entry.file.toPath().startsWith(moduleBuildDirectory) }
        }
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
    }
}

/**
 * Detekt without type resolution: `source` is set explicitly to every `src/<sourceSet>/kotlin`
 * directory this module has, so coverage does not depend on how the alpha discovers source sets
 * under the AGP 9 KMP library plugin or AGP 9 built-in Kotlin (ARCH §12.1). No baseline.
 */
internal fun Project.configureDetekt() {
    val detektConfigFile = rootProject.layout.projectDirectory.file("config/detekt/detekt.yml")
    val repoRoot = rootProject.layout.projectDirectory
    val moduleSourceDirectories = kotlinSourceDirectories()

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        allRules.set(false)
        parallel.set(true)
        ignoreFailures.set(false)
        autoCorrect.set(false)
        disableDefaultRuleSets.set(false)
        config.setFrom(detektConfigFile)
        basePath.set(repoRoot)
        source.setFrom(moduleSourceDirectories)
    }

    dependencies.add("detektPlugins", libs.findLibrary("compose-rules-detekt").get())
}

/** Every `src/<sourceSet>/kotlin` directory this module has, whatever the source-set names are. */
private fun Project.kotlinSourceDirectories(): List<File> =
    layout.projectDirectory
        .dir("src")
        .asFile
        .listFiles { file -> file.isDirectory }
        ?.map { sourceSetDir -> sourceSetDir.resolve("kotlin") }
        ?.filter { it.exists() }
        .orEmpty()
