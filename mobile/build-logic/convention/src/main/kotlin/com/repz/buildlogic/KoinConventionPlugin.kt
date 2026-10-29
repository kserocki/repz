package com.repz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Koin compiler plugin (`@Single`/`@Factory`/`@Module`) plus `koin-core` and `koin-annotations` for
 * a KMP module with DI definitions. The full graph is only verified at compile time in the module
 * that has the Koin entry point (`app:shared`); leaf modules get no compile-time diagnostics.
 */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(pluginManager.hasPlugin("repz.kmp.library")) {
                "repz.koin must be applied together with repz.kmp.library " +
                    "(apply repz.kmp.library before repz.koin in this module's plugins block)."
            }

            pluginManager.apply("io.insert-koin.compiler.plugin")

            extensions.getByType<KotlinMultiplatformExtension>().sourceSets.named("commonMain") {
                dependencies {
                    implementation(libs.findLibrary("koin-core").get())
                    implementation(libs.findLibrary("koin-annotations").get())
                }
            }
        }
    }
}
