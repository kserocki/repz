package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureScope
import com.repz.app.konsist.support.fixtureTextFiles
import com.repz.app.konsist.support.projectScope
import com.repz.app.konsist.support.realTextFiles
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class HeuristicRulesTest {
    @Test
    fun given_project_whenT1Checked_thenNoViolations() {
        checkT1NoHardcodedDesignTokens(realTextFiles())
    }

    @Test
    fun given_t1ViolationFixture_whenChecked_thenFailsWithT1() {
        val error = assertFails { checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1")) }

        assertContains(error.message.orEmpty(), "T1")
    }

    @Test
    fun given_project_whenS1Checked_thenNoViolations() {
        checkS1NoHardcodedUserFacingStrings(projectScope())
    }

    @Test
    fun given_s1ViolationFixture_whenChecked_thenFailsWithS1() {
        val error = assertFails { checkS1NoHardcodedUserFacingStrings(fixtureScope("s1")) }

        assertContains(error.message.orEmpty(), "S1")
    }

    @Test
    fun given_project_whenC1Checked_thenNoViolations() {
        checkC1OnlyLinearKeysInTodoForm(realTextFiles())
    }

    @Test
    fun given_c1ViolationFixture_whenChecked_thenFailsWithC1() {
        val error = assertFails { checkC1OnlyLinearKeysInTodoForm(fixtureTextFiles("c1")) }

        assertContains(error.message.orEmpty(), "C1")
    }
}
