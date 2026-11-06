import com.repz.buildlogic.i18n.GenerateIosAppLocalizationsTask
import com.repz.buildlogic.i18n.appLocalesSourceDirectory

plugins {
    id("repz.kmp.library")
    id("repz.compose")
    id("repz.koin")
    id("repz.room.consumer")
}

kotlin {
    // Static RepzShared framework, linked by Xcode's embedAndSignAppleFrameworkForXcode run-script
    // phase (ADR-0017): no CocoaPods/SPM, no dyld cost at launch.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "RepzShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.domain)
            implementation(projects.feature.auth.api)
            implementation(projects.feature.auth.impl)
            implementation(projects.feature.workout.api)
            implementation(projects.feature.workout.impl)
            implementation(projects.feature.exercises.api)
            implementation(projects.feature.exercises.impl)
            implementation(projects.feature.history.api)
            implementation(projects.feature.history.impl)
            implementation(projects.feature.templates.impl)
            implementation(projects.feature.settings.api)
            implementation(projects.feature.settings.impl)
            implementation(projects.core.data)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.sync)
            implementation(projects.core.navigation)
            implementation(projects.core.designsystem)
            implementation(projects.core.network)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

// Generates the iOS side of the canonical language list (ADR-0017) from this module's own
// composeResources/values-* folders, consumed by mobile/iosApp/scripts/apply-app-localizations.sh.
tasks.register<GenerateIosAppLocalizationsTask>("generateIosAppLocalizations") {
    sourceDirectory.set(project.appLocalesSourceDirectory)
    outputDirectory.set(layout.buildDirectory.dir("generated/iosAppLocalizations"))
}
