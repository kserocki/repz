package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureScope
import com.repz.app.konsist.support.projectScope
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class KoinRulesTest {
    @Test
    fun given_project_whenD1Checked_thenNoViolations() {
        checkD1ComponentScanConfiguredPerModule(projectScope())
    }

    @Test
    fun given_d1OverlappingScansFixture_whenChecked_thenFailsWithD1() {
        val error = assertFails { checkD1ComponentScanConfiguredPerModule(fixtureScope("d1")) }

        assertContains(error.message.orEmpty(), "D1")
    }

    @Test
    fun given_d1MissingArgumentFixture_whenChecked_thenFailsWithD1() {
        val error = assertFails { checkD1ComponentScanConfiguredPerModule(fixtureScope("d1-missing-argument")) }

        assertContains(error.message.orEmpty(), "D1")
    }

    @Test
    fun given_d1ScanNotOnModuleFixture_whenChecked_thenFailsWithD1() {
        val error = assertFails { checkD1ComponentScanConfiguredPerModule(fixtureScope("d1-scan-not-on-module")) }

        assertContains(error.message.orEmpty(), "D1")
    }

    @Test
    fun given_project_whenD2Checked_thenNoViolations() {
        checkD2AnnotatedClassesInsideOwnModuleScan(projectScope())
    }

    @Test
    fun given_d2ViolationFixture_whenChecked_thenFailsWithD2() {
        val error = assertFails { checkD2AnnotatedClassesInsideOwnModuleScan(fixtureScope("d2")) }

        assertContains(error.message.orEmpty(), "D2")
    }

    @Test
    fun given_d2SingletonFixture_whenChecked_thenFailsOnSingletonAnnotation() {
        val error = assertFails { checkD2AnnotatedClassesInsideOwnModuleScan(fixtureScope("d2-singleton")) }

        assertContains(error.message.orEmpty(), "@Singleton is forbidden")
    }

    @Test
    fun given_d2TestOnlyScanFixture_whenChecked_thenFailsWithD2() {
        val error = assertFails { checkD2AnnotatedClassesInsideOwnModuleScan(fixtureScope("d2-test-only-scan")) }

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

    @Test
    fun given_d5AllowedNamedQualifiersFixture_whenChecked_thenNoViolations() {
        checkD5OneOwnerPerDefinition(fixtureScope("d5-allowed-named-qualifiers"))
    }

    @Test
    fun given_d5AllowedPlatformActualFixture_whenChecked_thenNoViolations() {
        checkD5OneOwnerPerDefinition(fixtureScope("d5-allowed-platform-actual"))
    }

    @Test
    fun given_d5PlatformActualSameSourceSetFixture_whenChecked_thenFailsWithD5() {
        val error = assertFails { checkD5OneOwnerPerDefinition(fixtureScope("d5-platform-actual-same-source-set")) }

        assertContains(error.message.orEmpty(), "D5")
    }

    @Test
    fun given_d5AutoBindingFixture_whenChecked_thenFailsWithD5() {
        val error = assertFails { checkD5OneOwnerPerDefinition(fixtureScope("d5-auto-binding")) }

        assertContains(error.message.orEmpty(), "D5")
    }

    @Test
    fun given_d5AutoBindingExtraAnnotationFixture_whenChecked_thenFailsWithD5() {
        val error = assertFails { checkD5OneOwnerPerDefinition(fixtureScope("d5-auto-binding-extra-annotation")) }

        assertContains(error.message.orEmpty(), "D5")
    }

    @Test
    fun given_d5BindsListFixture_whenChecked_thenFailsWithD5() {
        val error = assertFails { checkD5OneOwnerPerDefinition(fixtureScope("d5-binds-list")) }

        assertContains(error.message.orEmpty(), "D5")
    }

    @Test
    fun given_project_whenD6Checked_thenNoViolations() {
        checkD6NoPassThroughProvider(projectScope())
    }

    @Test
    fun given_d6ViolationFixture_whenChecked_thenFailsWithD6() {
        val error = assertFails { checkD6NoPassThroughProvider(fixtureScope("d6")) }

        assertContains(error.message.orEmpty(), "D6")
        assertContains(error.message.orEmpty(), "annotate HistoryRepositoryImpl instead")
    }

    @Test
    fun given_d6AllowedFixture_whenChecked_thenNoViolations() {
        checkD6NoPassThroughProvider(fixtureScope("d6-allowed"))
    }

    @Test
    fun given_project_whenD7Checked_thenNoViolations() {
        checkD7DefinitionKindMatchesRole(projectScope())
    }

    @Test
    fun given_d7ViolationFixture_whenChecked_thenFailsWithD7() {
        val error = assertFails { checkD7DefinitionKindMatchesRole(fixtureScope("d7")) }

        assertContains(error.message.orEmpty(), "D7")
    }
}
