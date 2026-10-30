package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureScope
import com.repz.app.konsist.support.fixtureTextFiles
import kotlin.test.Test

/**
 * The app shell's known, accepted shape: `com.repz.app` package in `app:android`, a
 * `@Composable fun RepzRoot()` in `app:shared` with no `*Content` pair, and a `RepzBrandColors`
 * object with `Color(0x…)` values in `core:designsystem`. Proves P1/N4/T1 never flag it.
 */
class AppShellShapesAcceptanceTest {
    @Test
    fun given_appShellShapesFixture_whenP1Checked_thenNoViolations() {
        checkP1PackageMatchesModule(fixtureScope("app-shell-shapes-acceptance"))
    }

    @Test
    fun given_appShellShapesFixture_whenN4Checked_thenNoViolations() {
        checkN4ScreenContentPairing(fixtureScope("app-shell-shapes-acceptance"))
    }

    @Test
    fun given_appShellShapesFixture_whenT1Checked_thenNoViolations() {
        checkT1NoHardcodedDesignTokens(fixtureTextFiles("app-shell-shapes-acceptance"))
    }
}
