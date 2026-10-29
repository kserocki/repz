plugins {
    id("repz.kmp.library")
    id("repz.compose")
}

kotlin {
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
    }
}
