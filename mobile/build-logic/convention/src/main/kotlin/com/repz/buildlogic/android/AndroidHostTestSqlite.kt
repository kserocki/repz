package com.repz.buildlogic.android

import com.repz.buildlogic.libs
import org.gradle.api.Project

private const val ANDROID_HOST_TEST_RUNTIME_CLASSPATH = "androidHostTestRuntimeClasspath"
private const val SQLITE_BUNDLED_MODULE = "androidx.sqlite:sqlite-bundled"
private const val SQLITE_BUNDLED_JVM_MODULE = "androidx.sqlite:sqlite-bundled-jvm"

/**
 * Lets `BundledSQLiteDriver` load on the host JVM in Android host tests. The Android variant of
 * `androidx.sqlite:sqlite-bundled` ships only Android `.so` natives, so Room fails with
 * `UnsatisfiedLinkError: no sqliteJni` when a DAO test runs on `testAndroidHostTest`'s JVM; this
 * substitutes the host-JVM artifact at the same catalog version, scoped to that one configuration.
 */
internal fun Project.useJvmBundledSqliteInAndroidHostTests() {
    val sqliteVersion = libs.findVersion("sqlite").get().requiredVersion

    configurations.matching { it.name == ANDROID_HOST_TEST_RUNTIME_CLASSPATH }.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module(SQLITE_BUNDLED_MODULE))
                .using(module("$SQLITE_BUNDLED_JVM_MODULE:$sqliteVersion"))
                .because(
                    "sqlite-bundled ships Android-only natives; sqlite-bundled-jvm carries the " +
                        "host JVM natives that testAndroidHostTest runs on."
                )
        }
    }
}
