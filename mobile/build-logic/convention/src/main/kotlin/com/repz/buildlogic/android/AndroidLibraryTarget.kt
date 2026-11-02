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
 * from the catalog, the host test source set, Android resource/asset packaging, and a JVM 17
 * target.
 */
internal fun Project.configureAndroidLibraryTarget(kotlin: KotlinMultiplatformExtension) {
    val moduleNamespace = repzNamespace()
    val minSdkVersion = androidMinSdk
    val compileSdkVersion = androidCompileSdk

    kotlin.targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        namespace = moduleNamespace
        compileSdk = compileSdkVersion
        minSdk = minSdkVersion

        // AGP defaults `androidResources.enable` to false for KMP library targets (true for
        // plain android libraries), which skips the module's resource/asset merge and package
        // tasks entirely (AGP's KmpTaskManager gates MergeResources and the "package assets in
        // the library aar" tasks behind `buildFeatures.androidResources`). Compose Multiplatform
        // copies composeResources (fonts, strings, files) into the variant's assets source set,
        // so without this flag those files never reach the module's AAR and, from there, never
        // reach the app APK. Enabling it unconditionally is a no-op for modules with no
        // composeResources (AGP generates empty resource artifacts for them).
        androidResources {
            enable = true
        }

        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    registerComposeResourcesPackagingGuard()
}
