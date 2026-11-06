plugins {
    id("repz.kmp.library")
    id("repz.koin")
    id("repz.room.consumer")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.domain)
            implementation(projects.core.database)
            implementation(projects.core.network)
            implementation(projects.core.datastore)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
