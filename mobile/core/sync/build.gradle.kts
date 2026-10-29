plugins {
    id("repz.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.database)
            implementation(projects.core.network)
        }
    }
}
