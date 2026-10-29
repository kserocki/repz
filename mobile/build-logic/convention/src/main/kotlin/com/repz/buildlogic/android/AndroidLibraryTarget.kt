package com.repz.buildlogic.android

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.repz.buildlogic.repzNamespace
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures the Android target that `com.android.kotlin.multiplatform.library` registers on
 * `kotlin.targets` (the `android { }` block in build scripts, AGP >= 8.12): namespace, SDK levels
 * from the catalog, the host test source set, and a JVM 17 target.
 */
internal fun Project.configureAndroidLibraryTarget(kotlin: KotlinMultiplatformExtension) {
    val moduleNamespace = repzNamespace()
    val minSdkVersion = androidMinSdk
    val compileSdkVersion = androidCompileSdk

    kotlin.targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        namespace = moduleNamespace
        compileSdk = compileSdkVersion
        minSdk = minSdkVersion

        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
