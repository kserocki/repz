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
    fun given_t1ColorCallFixture_whenChecked_thenFailsOnColorCall() {
        val error = assertFails { checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-color-call")) }

        assertContains(error.message.orEmpty(), "no hard-coded Color(…) calls")
    }

    @Test
    fun given_t1ColorNamedFixture_whenChecked_thenFailsOnNamedColor() {
        val error = assertFails { checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-color-named")) }

        assertContains(error.message.orEmpty(), "only Color.Transparent/Color.Unspecified are allowed")
    }

    @Test
    fun given_t1DpFixture_whenChecked_thenFailsOnNonZeroDp() {
        val error = assertFails { checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-dp")) }

        assertContains(error.message.orEmpty(), "only 0.dp is an allowed hard-coded dp literal")
    }

    @Test
    fun given_t1SpFixture_whenChecked_thenFailsOnSpLiteral() {
        val error = assertFails { checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-sp")) }

        assertContains(error.message.orEmpty(), "no hard-coded sp literals")
    }

    @Test
    fun given_t1SpringFixture_whenChecked_thenFailsOnSpringWithTypeArgument() {
        val error = assertFails { checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-spring")) }

        assertContains(error.message.orEmpty(), "no hard-coded spring(/tween( calls")
    }

    @Test
    fun given_t1TweenFixture_whenChecked_thenFailsOnTweenWithTypeArgument() {
        val error = assertFails { checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-tween")) }

        assertContains(error.message.orEmpty(), "no hard-coded spring(/tween( calls")
    }

    @Test
    fun given_t1AllowedDpFixture_whenChecked_thenNoViolations() {
        checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-allowed-dp"))
    }

    @Test
    fun given_t1AllowedColorTransparentFixture_whenChecked_thenNoViolations() {
        checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-allowed-color-transparent"))
    }

    @Test
    fun given_t1AllowedColorUnspecifiedFixture_whenChecked_thenNoViolations() {
        checkT1NoHardcodedDesignTokens(fixtureTextFiles("t1-allowed-color-unspecified"))
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
    fun given_s1AllowedLabelFixture_whenChecked_thenNoViolations() {
        checkS1NoHardcodedUserFacingStrings(fixtureScope("s1-allowed-label"))
    }

    @Test
    fun given_project_whenC1Checked_thenNoViolations() {
        checkC1OnlyLinearKeysInTodoForm(realTextFiles())
    }

    @Test
    fun given_c1RepKeyBareFixture_whenChecked_thenFailsOnBareRepKey() {
        val error = assertFails { checkC1OnlyLinearKeysInTodoForm(fixtureTextFiles("c1-rep-key-bare")) }

        assertContains(error.message.orEmpty(), "C1")
    }

    @Test
    fun given_c1BareTodoFixture_whenChecked_thenFailsOnTodoWithoutKey() {
        val error = assertFails { checkC1OnlyLinearKeysInTodoForm(fixtureTextFiles("c1-bare-todo")) }

        assertContains(error.message.orEmpty(), "C1")
    }

    @Test
    fun given_c1BuildScriptFixture_whenChecked_thenFailsOnKtsComment() {
        val error = assertFails { checkC1OnlyLinearKeysInTodoForm(fixtureTextFiles("c1-build-script")) }

        assertContains(error.message.orEmpty(), "C1")
    }

    @Test
    fun given_c1AllowedTodoFixture_whenChecked_thenNoViolations() {
        checkC1OnlyLinearKeysInTodoForm(fixtureTextFiles("c1-allowed-todo"))
    }

    @Test
    fun given_c1AllowedUpstreamFixture_whenChecked_thenNoViolations() {
        checkC1OnlyLinearKeysInTodoForm(fixtureTextFiles("c1-allowed-upstream"))
    }
}
