plugins {
    id("repz.kmp.library")
    id("repz.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.domain)
            implementation(projects.core.database)
            implementation(projects.core.network)
            implementation(projects.core.datastore)
        }
        commonTest.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
