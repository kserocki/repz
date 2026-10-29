plugins {
    alias(libs.plugins.kotlin.jvm)
    id("repz.quality")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // A Test task is normally up-to-date once its classpath is unchanged; Konsist reads every
    // module's source text directly, so re-running it when any Kotlin/Gradle source changes
    // anywhere under `mobile/` needs this explicit input.
    inputs
        .files(
            fileTree(rootDir) {
                include("**/*.kt", "**/*.kts")
                exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
            },
        ).withPropertyName("repzSourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
