plugins {
    id("repz.kmp.library")
    id("repz.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.navigation3.runtime)
            api(libs.androidx.navigation3.ui)
            api(libs.androidx.savedstate)
            api(libs.kotlinx.serialization.core)
            implementation(libs.compose.runtime.saveable)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
