package com.repz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A `feature:x:impl` module: a KMP library with Compose, depending on its own `feature:x:api`
 * (derived from this module's path) plus the core modules every feature is allowed to see
 * (AGENTS.md §3: never `core:data`/`core:database`/`core:network`, never another `impl`).
 */
class KmpFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("repz.kmp.library")
            pluginManager.apply("repz.compose")

            // TODO(REP-7): add Koin, lifecycle and kotlinx-collections-immutable.
            check(path.endsWith(":impl")) {
                "repz.kmp.feature.impl expects a module path ending in `:impl`, got `$path`."
            }
            val apiPath = path.removeSuffix(":impl") + ":api"

            val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

            kotlin.sourceSets.named("commonMain") {
                dependencies {
                    implementation(project(apiPath))
                    implementation(project(":core:domain"))
                    implementation(project(":core:designsystem"))
                    implementation(project(":core:ui"))
                    implementation(project(":core:navigation"))
                }
            }

            kotlin.sourceSets.named("commonTest") {
                dependencies {
                    implementation(project(":core:testing"))
                }
            }
        }
    }
}
