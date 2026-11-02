package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureRoot
import com.repz.app.konsist.support.realProjectRoot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class BrandRulesTest {
    @Test
    fun given_project_whenB1Checked_thenNoViolations() {
        checkB1BrandColorsMatchPlatforms(realProjectRoot())
    }

    @Test
    fun given_matchingBrandColors_whenB1Checked_thenNoViolations() {
        checkB1BrandColorsMatchPlatforms(fixtureRoot("b1-matching"))
    }

    @Test
    fun given_androidColorDrift_whenB1aChecked_thenViolation() {
        val error = assertFails { checkB1aAndroidColorsMatchBrand(fixtureRoot("b1-android-drift")) }

        assertContains(error.message.orEmpty(), "must equal RepzBrandColors.Cyan")
    }

    @Test
    fun given_iosLaunchColorDrift_whenB1bChecked_thenViolation() {
        val error = assertFails { checkB1bIosLaunchBackgroundMatchesBrand(fixtureRoot("b1-ios-drift")) }

        assertContains(error.message.orEmpty(), "must equal RepzBrandColors.Ink")
    }
}
