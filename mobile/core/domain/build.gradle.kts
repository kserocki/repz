plugins {
    id("repz.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
        }
        // core:testing main -> core:domain main already exists (the fakes implement domain
        // interfaces); this is the reverse test -> main edge so core:domain's own tests can use
        // them. Not a cycle: Gradle resolves core:domain's own main output directly for this
        // module's test compilation, not through core:testing's transitive dependency on it.
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
