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
    fun given_a1DomainImportFixture_whenChecked_thenFailsOnDomainImport() {
        val error = assertFails { checkA1DomainAndCommonStayPure(fixtureTextFiles("a1-domain-import")) }

        assertContains(error.message.orEmpty(), "core:domain is pure Kotlin")
    }

    @Test
    fun given_a1CommonImportFixture_whenChecked_thenFailsOnCommonImport() {
        val error = assertFails { checkA1DomainAndCommonStayPure(fixtureTextFiles("a1-common-import")) }

        assertContains(error.message.orEmpty(), "core:common never imports Koin")
    }

    @Test
    fun given_a1BuildFileFixture_whenChecked_thenFailsOnBuildFileToken() {
        val error = assertFails { checkA1DomainAndCommonStayPure(fixtureTextFiles("a1-build-file")) }

        assertContains(error.message.orEmpty(), "build files apply no Koin/Compose/Room plugin")
    }

    @Test
    fun given_project_whenA2Checked_thenNoViolations() {
        checkA2FeatureImplIsolation(realTextFiles())
    }

    @Test
    fun given_a2ImplImportFixture_whenChecked_thenFailsOnImplImportingImpl() {
        val error = assertFails { checkA2FeatureImplIsolation(fixtureTextFiles("a2-impl-import")) }

        assertContains(error.message.orEmpty(), "feature:x:impl never imports another feature's :impl")
    }

    @Test
    fun given_a2ApiImportFixture_whenChecked_thenFailsOnApiImportingImpl() {
        val error = assertFails { checkA2FeatureImplIsolation(fixtureTextFiles("a2-api-import")) }

        assertContains(error.message.orEmpty(), "feature:x:api never imports a feature's :impl")
    }

    @Test
    fun given_a2BuildFileFixture_whenChecked_thenFailsOnBuildFileDependency() {
        val error = assertFails { checkA2FeatureImplIsolation(fixtureTextFiles("a2-build-file")) }

        assertContains(error.message.orEmpty(), "feature build files never depend on another feature's :impl")
    }

    @Test
    fun given_project_whenA3Checked_thenNoViolations() {
        checkA3FeatureSeesDomainOnly(realTextFiles())
    }

    @Test
    fun given_a3ImportFixture_whenChecked_thenFailsOnCoreDataImport() {
        val error = assertFails { checkA3FeatureSeesDomainOnly(fixtureTextFiles("a3-import")) }

        assertContains(error.message.orEmpty(), "feature impl/api modules see domain interfaces only")
    }

    @Test
    fun given_a3BuildFileFixture_whenChecked_thenFailsOnBuildFileDependency() {
        val error = assertFails { checkA3FeatureSeesDomainOnly(fixtureTextFiles("a3-build-file")) }

        assertContains(error.message.orEmpty(), "feature build files never depend on core:data")
    }

    @Test
    fun given_project_whenA4Checked_thenNoViolations() {
        checkA4CompositionRootIsAppShared(realTextFiles())
    }

    @Test
    fun given_a4CompositionRootFixture_whenChecked_thenFailsOnNavDisplayOutsideAppShared() {
        val error = assertFails { checkA4CompositionRootIsAppShared(fixtureTextFiles("a4-composition-root")) }

        assertContains(error.message.orEmpty(), "used only in app:shared, the composition root")
    }

    @Test
    fun given_a4TrailingLambdaFixture_whenChecked_thenFailsOnStartKoinTrailingLambda() {
        val error = assertFails { checkA4CompositionRootIsAppShared(fixtureTextFiles("a4-trailing-lambda")) }

        assertContains(error.message.orEmpty(), "used only in app:shared, the composition root")
    }

    @Test
    fun given_a4CoreImportFixture_whenChecked_thenFailsOnCoreImportingFeature() {
        val error = assertFails { checkA4CompositionRootIsAppShared(fixtureTextFiles("a4-core-import")) }

        assertContains(error.message.orEmpty(), "core:* never imports feature or app:shared code")
    }

    @Test
    fun given_project_whenA5Checked_thenNoViolations() {
        checkA5EntitiesAndDtosStayInData(realTextFiles())
    }

    @Test
    fun given_a5EntityFixture_whenChecked_thenFailsOnEntityOutsideDatabase() {
        val error = assertFails { checkA5EntitiesAndDtosStayInData(fixtureTextFiles("a5-entity")) }

        assertContains(error.message.orEmpty(), "@Entity classes live only in core:database")
    }

    @Test
    fun given_a5DtoFixture_whenChecked_thenFailsOnDtoOutsideNetwork() {
        val error = assertFails { checkA5EntitiesAndDtosStayInData(fixtureTextFiles("a5-dto")) }

        assertContains(error.message.orEmpty(), "*Dto classes live only in core:network")
    }

    @Test
    fun given_a5SubpackageImportFixture_whenChecked_thenFailsOnSubpackagedEntityImport() {
        val error = assertFails { checkA5EntitiesAndDtosStayInData(fixtureTextFiles("a5-subpackage-import")) }

        assertContains(error.message.orEmpty(), "entity/DTO imports occur only in core:database/network/data/sync")
    }
}
