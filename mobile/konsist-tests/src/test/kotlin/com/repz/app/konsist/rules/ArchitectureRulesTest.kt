package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureTextFiles
import com.repz.app.konsist.support.realTextFiles
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class ArchitectureRulesTest {
    @Test
    fun given_project_whenA1Checked_thenNoViolations() {
        checkA1DomainAndCommonStayPure(realTextFiles())
    }

    @Test
    fun given_a1ViolationFixture_whenChecked_thenFailsWithA1() {
        val error = assertFails { checkA1DomainAndCommonStayPure(fixtureTextFiles("a1")) }

        assertContains(error.message.orEmpty(), "A1")
    }

    @Test
    fun given_project_whenA2Checked_thenNoViolations() {
        checkA2FeatureImplIsolation(realTextFiles())
    }

    @Test
    fun given_a2ViolationFixture_whenChecked_thenFailsWithA2() {
        val error = assertFails { checkA2FeatureImplIsolation(fixtureTextFiles("a2")) }

        assertContains(error.message.orEmpty(), "A2")
    }

    @Test
    fun given_project_whenA3Checked_thenNoViolations() {
        checkA3FeatureSeesDomainOnly(realTextFiles())
    }

    @Test
    fun given_a3ViolationFixture_whenChecked_thenFailsWithA3() {
        val error = assertFails { checkA3FeatureSeesDomainOnly(fixtureTextFiles("a3")) }

        assertContains(error.message.orEmpty(), "A3")
    }

    @Test
    fun given_project_whenA4Checked_thenNoViolations() {
        checkA4CompositionRootIsAppShared(realTextFiles())
    }

    @Test
    fun given_a4ViolationFixture_whenChecked_thenFailsWithA4() {
        val error = assertFails { checkA4CompositionRootIsAppShared(fixtureTextFiles("a4")) }

        assertContains(error.message.orEmpty(), "A4")
    }

    @Test
    fun given_project_whenA5Checked_thenNoViolations() {
        checkA5EntitiesAndDtosStayInData(realTextFiles())
    }

    @Test
    fun given_a5ViolationFixture_whenChecked_thenFailsWithA5() {
        val error = assertFails { checkA5EntitiesAndDtosStayInData(fixtureTextFiles("a5")) }

        assertContains(error.message.orEmpty(), "A5")
    }
}
