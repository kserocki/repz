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
}

// This module builds `repz.quality`, so it cannot apply it; ktlint and Detekt are configured here
// by hand, with the same `.editorconfig` and `config/detekt/detekt.yml` as every other module.
ktlint {
    version.set(libs.versions.ktlint.asProvider())
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
}

detekt {
    buildUponDefaultConfig.set(true)
    allRules.set(false)
    parallel.set(true)
    ignoreFailures.set(false)
    autoCorrect.set(false)
    disableDefaultRuleSets.set(false)
    config.setFrom(rootProject.layout.projectDirectory.dir("..").file("config/detekt/detekt.yml"))
    basePath.set(rootProject.layout.projectDirectory.dir(".."))
    source.setFrom(layout.projectDirectory.dir("src/main/kotlin"))
}

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
