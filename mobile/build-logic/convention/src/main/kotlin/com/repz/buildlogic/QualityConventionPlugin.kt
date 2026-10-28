package com.repz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Placeholder, intentionally a no-op. Already applied everywhere by `repz.kmp.library` and
 * `repz.android.application`, so REP-9 only fills this plugin's body (ktlint, Detekt with Compose
 * rules) without touching any module's build file. Konsist lives in its own `konsist-tests`
 * module, created in REP-9.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // TODO(REP-9): apply ktlint and Detekt with Compose rules.
    }
}
