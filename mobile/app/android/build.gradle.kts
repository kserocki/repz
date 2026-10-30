import com.repz.buildlogic.android.registerGenerateAndroidAppNameResources

plugins {
    id("repz.android.application")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.repz.app"

    defaultConfig {
        applicationId = "com.repz.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(projects.app.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
}

registerGenerateAndroidAppNameResources()
