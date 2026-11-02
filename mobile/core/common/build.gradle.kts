plugins {
    id("repz.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
    }
}
