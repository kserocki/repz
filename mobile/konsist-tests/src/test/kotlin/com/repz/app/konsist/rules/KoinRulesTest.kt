package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureScope
import com.repz.app.konsist.support.projectScope
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class KoinRulesTest {
    @Test
    fun given_project_whenD1Checked_thenNoViolations() {
        checkD1ComponentScanOnlyInAppShared(projectScope())
    }

    @Test
    fun given_d1ViolationFixture_whenChecked_thenFailsWithD1() {
        val error = assertFails { checkD1ComponentScanOnlyInAppShared(fixtureScope("d1")) }

        assertContains(error.message.orEmpty(), "D1")
    }

    @Test
    fun given_project_whenD2Checked_thenNoViolations() {
        checkD2KoinDefinitionClassesOnlyInAppShared(projectScope())
    }

    @Test
    fun given_d2ViolationFixture_whenChecked_thenFailsWithD2() {
        val error = assertFails { checkD2KoinDefinitionClassesOnlyInAppShared(fixtureScope("d2")) }

        assertContains(error.message.orEmpty(), "D2")
    }

    @Test
    fun given_project_whenD3Checked_thenNoViolations() {
        checkD3AtMostOneModuleClassPerModule(projectScope())
    }

    @Test
    fun given_d3ViolationFixture_whenChecked_thenFailsWithD3() {
        val error = assertFails { checkD3AtMostOneModuleClassPerModule(fixtureScope("d3")) }

        assertContains(error.message.orEmpty(), "D3")
    }

    @Test
    fun given_project_whenD4Checked_thenNoViolations() {
        checkD4ProviderFunctionsDeclareReturnType(projectScope())
    }

    @Test
    fun given_d4ViolationFixture_whenChecked_thenFailsWithD4() {
        val error = assertFails { checkD4ProviderFunctionsDeclareReturnType(fixtureScope("d4")) }

        assertContains(error.message.orEmpty(), "D4")
    }

    @Test
    fun given_project_whenD5Checked_thenNoViolations() {
        checkD5OneOwnerPerDefinition(projectScope())
    }

    @Test
    fun given_d5ViolationFixture_whenChecked_thenFailsWithD5() {
        val error = assertFails { checkD5OneOwnerPerDefinition(fixtureScope("d5")) }

        assertContains(error.message.orEmpty(), "D5")
    }
}
