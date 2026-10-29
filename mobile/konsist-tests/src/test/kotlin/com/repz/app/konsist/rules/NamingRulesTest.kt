package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureScope
import com.repz.app.konsist.support.projectScope
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class NamingRulesTest {
    @Test
    fun given_project_whenN1Checked_thenNoViolations() {
        checkN1ViewModelShapeAndNaming(projectScope())
    }

    @Test
    fun given_n1ViolationFixture_whenChecked_thenFailsWithN1() {
        val error = assertFails { checkN1ViewModelShapeAndNaming(fixtureScope("n1")) }

        assertContains(error.message.orEmpty(), "N1")
    }

    @Test
    fun given_project_whenN2Checked_thenNoViolations() {
        checkN2UiStatePropertiesAreImmutableCollections(projectScope())
    }

    @Test
    fun given_n2ViolationFixture_whenChecked_thenFailsWithN2() {
        val error = assertFails { checkN2UiStatePropertiesAreImmutableCollections(fixtureScope("n2")) }

        assertContains(error.message.orEmpty(), "N2")
    }

    @Test
    fun given_project_whenN3Checked_thenNoViolations() {
        checkN3UseCaseShape(projectScope())
    }

    @Test
    fun given_n3ViolationFixture_whenChecked_thenFailsWithN3() {
        val error = assertFails { checkN3UseCaseShape(fixtureScope("n3")) }

        assertContains(error.message.orEmpty(), "N3")
    }

    @Test
    fun given_project_whenN4Checked_thenNoViolations() {
        checkN4ScreenContentPairing(projectScope())
    }

    @Test
    fun given_n4ViolationFixture_whenChecked_thenFailsWithN4() {
        val error = assertFails { checkN4ScreenContentPairing(fixtureScope("n4")) }

        assertContains(error.message.orEmpty(), "N4")
    }

    @Test
    fun given_project_whenN5Checked_thenNoViolations() {
        checkN5RepositoryShape(projectScope())
    }

    @Test
    fun given_n5ViolationFixture_whenChecked_thenFailsWithN5() {
        val error = assertFails { checkN5RepositoryShape(fixtureScope("n5")) }

        assertContains(error.message.orEmpty(), "N5")
    }

    @Test
    fun given_project_whenN6Checked_thenNoViolations() {
        checkN6TestFunctionNaming(projectScope())
    }

    @Test
    fun given_n6ViolationFixture_whenChecked_thenFailsWithN6() {
        val error = assertFails { checkN6TestFunctionNaming(fixtureScope("n6")) }

        assertContains(error.message.orEmpty(), "N6")
    }

    @Test
    fun given_project_whenN7Checked_thenNoViolations() {
        checkN7NoMockingLibraries(projectScope())
    }

    @Test
    fun given_n7ViolationFixture_whenChecked_thenFailsWithN7() {
        val error = assertFails { checkN7NoMockingLibraries(fixtureScope("n7")) }

        assertContains(error.message.orEmpty(), "N7")
    }

    @Test
    fun given_project_whenV1Checked_thenNoViolations() {
        checkV1FeatureApiShape(projectScope())
    }

    @Test
    fun given_v1ViolationFixture_whenChecked_thenFailsWithV1() {
        val error = assertFails { checkV1FeatureApiShape(fixtureScope("v1")) }

        assertContains(error.message.orEmpty(), "V1")
    }
}
