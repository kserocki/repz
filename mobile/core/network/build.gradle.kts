import com.repz.buildlogic.contract.registerGenerateContractFixtures

plugins {
    id("repz.kmp.library")
    id("repz.koin")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            api(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        commonTest {
            kotlin.srcDir(registerGenerateContractFixtures())
        }
    }
}

// The live API smoke test (androidHostTest's LiveApiSmokeTest) needs a running API and is never
// part of the normal gates: excluded from every Test task unless -Prepz.liveApiUrl=<url> is passed
// (Spotter's manual test plan, L1-L6). providers.gradleProperty keeps this configuration-cache safe.
private val liveApiUrl = providers.gradleProperty("repz.liveApiUrl")
private val liveApiAccessTtlSeconds = providers.gradleProperty("repz.liveApiAccessTtlSeconds").orElse("900")

tasks.withType<Test>().configureEach {
    systemProperty("repz.liveApiUrl", liveApiUrl.getOrElse(""))
    systemProperty("repz.liveApiAccessTtlSeconds", liveApiAccessTtlSeconds.get())
    if (!liveApiUrl.isPresent) {
        filter {
            excludeTestsMatching("com.repz.app.core.network.livesmoke.LiveApiSmokeTest")
        }
    }
}
