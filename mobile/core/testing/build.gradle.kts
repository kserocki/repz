plugins {
    id("repz.kmp.library")
    id("repz.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
        }
    }
}
