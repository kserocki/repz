package com.repz.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.repz.buildlogic.android.androidCompileSdk
import com.repz.buildlogic.android.androidMinSdk
import com.repz.buildlogic.android.androidTargetSdk
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applies `com.android.application` and `repz.quality`, and sets the SDK levels and Java 17
 * compatibility shared by every Android application module. AGP 9's built-in Kotlin support
 * compiles Kotlin sources without `org.jetbrains.kotlin.android`; its `jvmTarget` defaults from
 * `compileOptions.targetCompatibility`, so no separate `kotlin { }` block is needed here.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")
        target.pluginManager.apply("repz.quality")

        val minSdkVersion = target.androidMinSdk
        val compileSdkVersion = target.androidCompileSdk
        val targetSdkVersion = target.androidTargetSdk

        target.extensions.configure<ApplicationExtension> {
            compileSdk = compileSdkVersion

            defaultConfig {
                minSdk = minSdkVersion
                targetSdk = targetSdkVersion
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
