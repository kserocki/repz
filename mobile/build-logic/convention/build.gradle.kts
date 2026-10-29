plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.androidx.room3.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "repz.kmp.library"
            implementationClass = "com.repz.buildlogic.KmpLibraryConventionPlugin"
        }
        register("room") {
            id = "repz.room"
            implementationClass = "com.repz.buildlogic.RoomConventionPlugin"
        }
        register("compose") {
            id = "repz.compose"
            implementationClass = "com.repz.buildlogic.ComposeConventionPlugin"
        }
        register("koin") {
            id = "repz.koin"
            implementationClass = "com.repz.buildlogic.KoinConventionPlugin"
        }
        register("kmpFeatureApi") {
            id = "repz.kmp.feature.api"
            implementationClass = "com.repz.buildlogic.KmpFeatureApiConventionPlugin"
        }
        register("kmpFeatureImpl") {
            id = "repz.kmp.feature.impl"
            implementationClass = "com.repz.buildlogic.KmpFeatureImplConventionPlugin"
        }
        register("androidApplication") {
            id = "repz.android.application"
            implementationClass = "com.repz.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("quality") {
            id = "repz.quality"
            implementationClass = "com.repz.buildlogic.QualityConventionPlugin"
        }
    }
}
