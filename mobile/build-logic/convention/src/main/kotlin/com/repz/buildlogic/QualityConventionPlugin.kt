package com.repz.buildlogic

import com.repz.buildlogic.quality.configureDetekt
import com.repz.buildlogic.quality.configureKtlint
import org.gradle.api.Plugin
import org.gradle.api.Project

/** ktlint (formatting) and Detekt with the Compose rules (static analysis), both part of `check`. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            pluginManager.apply("dev.detekt")

            configureKtlint()
            configureDetekt()
        }
    }
}
