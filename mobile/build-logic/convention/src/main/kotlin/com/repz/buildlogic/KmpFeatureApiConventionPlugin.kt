package com.repz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A `feature:x:api` module: a KMP library (`repz.kmp.library`) plus kotlinx.serialization for
 * `@Serializable` NavKeys, depending on `core:navigation`.
 */
class KmpFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("repz.kmp.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.getByType<KotlinMultiplatformExtension>().sourceSets.named("commonMain") {
                dependencies {
                    api(project(":core:navigation"))
                }
            }
        }
    }
}
