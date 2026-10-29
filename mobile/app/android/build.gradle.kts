plugins {
    id("repz.android.application")
}

android {
    namespace = "com.repz.app"

    defaultConfig {
        applicationId = "com.repz.app"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.app.shared)
}
