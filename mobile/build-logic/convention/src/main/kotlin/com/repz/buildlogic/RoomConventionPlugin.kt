package com.repz.buildlogic

import androidx.room3.gradle.RoomExtension
import com.repz.buildlogic.android.useJvmBundledSqliteInAndroidHostTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Room 3 (KSP-generated, bundled SQLite driver) for a `repz.kmp.library` module. KSP is wired
 * only to the three targets `repz.kmp.library` declares; a target added later without KSP fails
 * at compile time, since the generated `actual` would be missing.
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(pluginManager.hasPlugin("repz.kmp.library")) {
                "repz.room must be applied together with repz.kmp.library " +
                    "(apply repz.kmp.library before repz.room in this module's plugins block)."
            }

            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room3")

            extensions.configure<RoomExtension> {
                schemaDirectory(layout.projectDirectory.dir("schemas"))
            }

            extensions.getByType<KotlinMultiplatformExtension>().sourceSets.named("commonMain") {
                dependencies {
                    api(libs.findLibrary("androidx-room3-runtime").get())
                    implementation(libs.findLibrary("androidx-sqlite-bundled").get())
                }
            }

            val roomCompiler = libs.findLibrary("androidx-room3-compiler").get()
            dependencies.add("kspAndroid", roomCompiler)
            dependencies.add("kspIosArm64", roomCompiler)
            dependencies.add("kspIosSimulatorArm64", roomCompiler)

            useJvmBundledSqliteInAndroidHostTests()
        }
    }
}
