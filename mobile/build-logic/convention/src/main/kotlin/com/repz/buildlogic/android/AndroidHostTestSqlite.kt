package com.repz.buildlogic.android

import com.repz.buildlogic.libs
import org.gradle.api.Project

private const val ANDROID_HOST_TEST_RUNTIME_CLASSPATH = "androidHostTestRuntimeClasspath"
private const val SQLITE_BUNDLED_MODULE = "androidx.sqlite:sqlite-bundled"
private const val SQLITE_BUNDLED_JVM_MODULE = "androidx.sqlite:sqlite-bundled-jvm"

/**
 * Lets `BundledSQLiteDriver` load on the host JVM in Android host tests: `sqlite-bundled` ships
 * only Android natives, so it is substituted with `sqlite-bundled-jvm` on
 * `androidHostTestRuntimeClasspath` only.
 */
internal fun Project.useJvmBundledSqliteInAndroidHostTests() {
    val sqliteVersion = libs.findVersion("sqlite").get().requiredVersion

    configurations.matching { it.name == ANDROID_HOST_TEST_RUNTIME_CLASSPATH }.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module(SQLITE_BUNDLED_MODULE))
                .using(module("$SQLITE_BUNDLED_JVM_MODULE:$sqliteVersion"))
                .because(
                    "sqlite-bundled ships Android-only natives; sqlite-bundled-jvm carries the " +
                        "host JVM natives that testAndroidHostTest runs on.",
                )
        }
    }
}
