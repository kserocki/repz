import com.repz.buildlogic.i18n.GenerateIosAppLocalizationsTask
import com.repz.buildlogic.i18n.appLocalesSourceDirectory

plugins {
    id("repz.kmp.library")
    id("repz.compose")
    id("repz.koin")
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
            implementation(projects.feature.auth.impl)
            implementation(projects.feature.workout.impl)
            implementation(projects.feature.exercises.impl)
            implementation(projects.feature.history.impl)
            implementation(projects.feature.templates.impl)
            implementation(projects.feature.settings.impl)
            implementation(projects.core.data)
            implementation(projects.core.sync)
            implementation(projects.core.navigation)
            implementation(projects.core.designsystem)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}

// Generates the iOS side of the canonical language list (ADR-0017) from this module's own
// composeResources/values-* folders, consumed by mobile/iosApp/scripts/apply-app-localizations.sh.
tasks.register<GenerateIosAppLocalizationsTask>("generateIosAppLocalizations") {
    sourceDirectory.set(project.appLocalesSourceDirectory)
    outputDirectory.set(layout.buildDirectory.dir("generated/iosAppLocalizations"))
}
