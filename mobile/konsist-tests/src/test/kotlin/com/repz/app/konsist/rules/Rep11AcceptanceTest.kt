package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureScope
import com.repz.app.konsist.support.fixtureTextFiles
import kotlin.test.Test

/**
 * REP-11 (app shells) is expected to land, among other things: `com.repz.app` package in
 * `app:android`, a `@Composable fun RepzRoot()` in `app:shared` with no `*Content` pair, and a
 * `RepzBrandColors` object with `Color(0x…)` values in `core:designsystem`. This fixture proves none
 * of P1/N4/T1 flag that known, accepted shape (plan §3.8, WP-2 step 3a).
 */
class Rep11AcceptanceTest {
    @Test
    fun given_rep11KnownShapesFixture_whenP1Checked_thenNoViolations() {
        checkP1PackageMatchesModule(fixtureScope("rep11-acceptance"))
    }

    @Test
    fun given_rep11KnownShapesFixture_whenN4Checked_thenNoViolations() {
        checkN4ScreenContentPairing(fixtureScope("rep11-acceptance"))
    }

    @Test
    fun given_rep11KnownShapesFixture_whenT1Checked_thenNoViolations() {
        checkT1NoHardcodedDesignTokens(fixtureTextFiles("rep11-acceptance"))
    }
}
