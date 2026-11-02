package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureTextFiles
import com.repz.app.konsist.support.realTextFiles
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class InjectionRulesTest {
    @Test
    fun given_project_whenI1Checked_thenNoViolations() {
        checkI1TimeAndIdsAreInjected(realTextFiles())
    }

    @Test
    fun given_i1ClockSystemFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-clock-system")) }

        assertContains(error.message.orEmpty(), "I1: Clock.System is read only in the composition root's CommonModule")
    }

    @Test
    fun given_i1UuidRandomFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-uuid-random")) }

        assertContains(error.message.orEmpty(), "I1: Uuid.random/generateV4/generateV7 are called only in the id generator")
    }

    @Test
    fun given_i1UuidGenerateV4Fixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-uuid-generate-v4")) }

        assertContains(error.message.orEmpty(), "I1: Uuid.random/generateV4/generateV7 are called only in the id generator")
    }

    @Test
    fun given_i1AllowedClockSystemFixture_whenChecked_thenNoViolations() {
        checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-allowed-clock-system"))
    }

    @Test
    fun given_i1AllowedUuidGenerationFixture_whenChecked_thenNoViolations() {
        checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-allowed-uuid-generation"))
    }
}
