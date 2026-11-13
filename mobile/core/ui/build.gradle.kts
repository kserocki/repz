plugins {
    id("repz.kmp.library")
    id("repz.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}
