plugins {
    id("repz.kmp.library")
    id("repz.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The only module allowed to depend on Material3 (ADR-0006).
            api(libs.compose.material3)
        }
    }
}
