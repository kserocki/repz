plugins {
    id("repz.kmp.library")
    id("repz.compose")
}

// The generated Res class defaults to internal visibility; app:shared reads the nav bar's
// drawable resources from here, so it must be public.
compose.resources {
    publicResClass = true
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The only module allowed to depend on Material3 (ADR-0006).
            api(libs.compose.material3)
            // ImmutableList parameters on components such as RepzConnectedButtonGroup and
            // RepzFabMenu; exposed as api so callers can pass their own lists.
            api(libs.kotlinx.collections.immutable)
        }
        androidMain.dependencies {
            // Android haptics actual: ViewCompat/HapticFeedbackConstantsCompat (MOTION.md §3).
            implementation(libs.androidx.core)
        }
    }
}
