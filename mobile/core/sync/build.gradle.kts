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
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
