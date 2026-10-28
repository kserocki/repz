package com.repz.buildlogic

import com.repz.buildlogic.android.configureAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base convention for every shared KMP module: Kotlin Multiplatform plus the AGP 9 Android KMP
 * library target (delegated to Bolt's `configureAndroidLibraryTarget`) and the two Apple-silicon
 * iOS targets. Also applies `repz.quality`, whose body is filled in REP-9.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("repz.quality")

            group = repzGroup()

            extensions.configure<KotlinMultiplatformExtension> {
                iosArm64()
                iosSimulatorArm64()

                configureAndroidLibraryTarget(this)

                sourceSets.named("commonTest") {
                    dependencies {
                        implementation(libs.findLibrary("kotlin-test").get())
                    }
                }
            }
        }
    }
}
