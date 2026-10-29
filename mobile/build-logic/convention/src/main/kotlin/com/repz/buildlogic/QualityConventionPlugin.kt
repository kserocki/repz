package com.repz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Placeholder, intentionally a no-op; applied by the base plugins so quality tooling plugs in
 * without touching any module's build file.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // TODO(REP-9): apply ktlint and Detekt with Compose rules.
    }
}
