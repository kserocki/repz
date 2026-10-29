// Declares every plugin applied by a build-logic convention plugin so all subprojects agree on one
// version. Convention plugins apply the matching `id(...)` without a version (see build-logic).
plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint)
}

// `base` only gives this root project a `check` task; it has no Kotlin source of its own besides
// this script and `settings.gradle.kts` (linted by ktlint above). `build-logic` is a separate
// included build with its own lifecycle tasks, so its quality gate is wired in here too.
tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
