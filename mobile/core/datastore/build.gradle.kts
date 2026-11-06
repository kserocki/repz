plugins {
    id("repz.kmp.library")
}

kotlin {
    // Device tests run against a real file-backed DataStore on the emulator's filesystem.
    // Registered before `sourceSets` below, because the `androidDeviceTest` source set only exists
    // after this block.
    android {
        withDeviceTestBuilder {
            // Default name/source set ("deviceTest" / "androidDeviceTest"); only the runner differs.
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        // `androidDeviceTest` has no pre-generated `by KotlinSourceSetConvention` accessor (unlike
        // `androidMain`/`commonTest`): it's AGP's own KMP device test source set, not one of Kotlin
        // Gradle Plugin's own conventions, so it's looked up by name instead.
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}
