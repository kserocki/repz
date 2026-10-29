package com.repz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compose Multiplatform for a KMP module. Coordinates are declared directly from the catalog, not
 * through the `compose.*` accessor DSL (deprecated in CMP 1.12). Material3 is intentionally not
 * here: only `core:designsystem` declares it (ADR-0006).
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(pluginManager.hasPlugin("repz.kmp.library")) {
                "repz.compose must be applied together with repz.kmp.library " +
                    "(apply repz.kmp.library before repz.compose in this module's plugins block)."
            }

            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.getByType<KotlinMultiplatformExtension>().sourceSets.named("commonMain") {
                dependencies {
                    implementation(libs.findLibrary("compose-runtime").get())
                    implementation(libs.findLibrary("compose-foundation").get())
                    implementation(libs.findLibrary("compose-ui").get())
                    implementation(libs.findLibrary("compose-components-resources").get())
                    implementation(libs.findLibrary("compose-ui-tooling-preview").get())
                }
            }
        }
    }
}
