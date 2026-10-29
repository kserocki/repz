plugins {
    id("repz.kmp.library")
    id("repz.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
        }
    }
}
