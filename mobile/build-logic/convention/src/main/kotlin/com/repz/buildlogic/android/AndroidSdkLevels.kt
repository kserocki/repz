package com.repz.buildlogic.android

import com.repz.buildlogic.libs
import org.gradle.api.Project

/** Minimum Android API level, from `android-minSdk` in the version catalog (ADR-0014: API 29). */
internal val Project.androidMinSdk: Int
    get() = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

/** Compile-time Android API level, from `android-compileSdk` in the version catalog. */
internal val Project.androidCompileSdk: Int
    get() = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()

/** Target Android API level, from `android-targetSdk` in the version catalog. Application modules only. */
internal val Project.androidTargetSdk: Int
    get() = libs.findVersion("android-targetSdk").get().requiredVersion.toInt()
