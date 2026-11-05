package com.repz.buildlogic

import com.repz.buildlogic.android.useJvmBundledSqliteInAndroidHostTests
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * For a `repz.kmp.library` module that builds a real `RepzDatabase` in its own tests (e.g.
 * `core:data`'s repository tests) without owning any Room entities or DAOs itself: only the host
 * JVM bundled-SQLite substitution `repz.room` applies to its android host tests, without KSP or
 * the Room Gradle plugin (which only the module that declares the database needs).
 */
class RoomConsumerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.useJvmBundledSqliteInAndroidHostTests()
    }
}
