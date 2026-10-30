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
    fun given_n1ExtendsViewModelWrongNameFixture_whenChecked_thenFailsOnNaming() {
        val error = assertFails { checkN1ViewModelShapeAndNaming(fixtureScope("n1-extends-viewmodel-wrong-name")) }

        assertContains(error.message.orEmpty(), "a class extending ViewModel is named *ViewModel")
    }

    @Test
    fun given_n1NameWithoutExtendsFixture_whenChecked_thenFailsOnMissingParent() {
        val error = assertFails { checkN1ViewModelShapeAndNaming(fixtureScope("n1-name-without-extends")) }

        assertContains(error.message.orEmpty(), "a class named *ViewModel extends ViewModel")
    }

    @Test
    fun given_n1WrongLocationFixture_whenChecked_thenFailsOnPackageOrModule() {
        val error = assertFails { checkN1ViewModelShapeAndNaming(fixtureScope("n1-wrong-location")) }

        assertContains(error.message.orEmpty(), "a ViewModel lives in a ..presentation.. package")
    }

    @Test
    fun given_n1MissingMembersFixture_whenChecked_thenFailsOnMissingStateEffectsOnIntent() {
        val error = assertFails { checkN1ViewModelShapeAndNaming(fixtureScope("n1-missing-members")) }

        assertContains(error.message.orEmpty(), "XViewModel exposes state, effects and fun onIntent")
    }

    @Test
    fun given_n1MissingSiblingsFixture_whenChecked_thenFailsOnMissingUiStateIntentEffect() {
        val error = assertFails { checkN1ViewModelShapeAndNaming(fixtureScope("n1-missing-siblings")) }

        assertContains(error.message.orEmpty(), "XViewModel has XUiState/XIntent/XEffect in the same package")
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
    fun given_n3LocationFixture_whenChecked_thenFailsOnWrongModule() {
        val error = assertFails { checkN3UseCaseShape(fixtureScope("n3-location")) }

        assertContains(error.message.orEmpty(), "*UseCase classes live in core:domain")
    }

    @Test
    fun given_n3InvokeShapeFixture_whenChecked_thenFailsOnWrongFunctionShape() {
        val error = assertFails { checkN3UseCaseShape(fixtureScope("n3-invoke-shape")) }

        assertContains(error.message.orEmpty(), "*UseCase declares exactly one non-private function")
    }

    @Test
    fun given_project_whenN4Checked_thenNoViolations() {
        checkN4ScreenContentPairing(projectScope())
    }

    @Test
    fun given_n4ScreenContentPairingFixture_whenChecked_thenFailsOnMissingContent() {
        val error = assertFails { checkN4ScreenContentPairing(fixtureScope("n4-screen-content-pairing")) }

        assertContains(error.message.orEmpty(), "XScreen has a matching XContent in the same package")
    }

    @Test
    fun given_n4ContentShapeFixture_whenChecked_thenFailsOnMissingStateOrOnIntent() {
        val error = assertFails { checkN4ScreenContentPairing(fixtureScope("n4-content-shape")) }

        assertContains(error.message.orEmpty(), "XContent takes state and onIntent")
    }

    @Test
    fun given_n4KoinViewModelOutsideScreenFixture_whenChecked_thenFailsOnCallOutsideScreen() {
        val error = assertFails { checkN4ScreenContentPairing(fixtureScope("n4-koinviewmodel-outside-screen")) }

        assertContains(error.message.orEmpty(), "koinViewModel( is called only inside *Screen functions")
    }

    @Test
    fun given_n4MissingPreviewFixture_whenChecked_thenFailsOnNoPreviewCallingContent() {
        val error = assertFails { checkN4ScreenContentPairing(fixtureScope("n4-missing-preview")) }

        assertContains(error.message.orEmpty(), "at least one @Preview function in the same file calls XContent(")
    }

    @Test
    fun given_project_whenN5Checked_thenNoViolations() {
        checkN5RepositoryShape(projectScope())
    }

    @Test
    fun given_n5InterfaceLocationFixture_whenChecked_thenFailsOnInterfaceOutsideDomain() {
        val error = assertFails { checkN5RepositoryShape(fixtureScope("n5-interface-location")) }

        assertContains(error.message.orEmpty(), "*Repository interfaces live in core:domain")
    }

    @Test
    fun given_n5ImplementationNamingFixture_whenChecked_thenFailsOnWrongImplementationName() {
        val error = assertFails { checkN5RepositoryShape(fixtureScope("n5-implementation-naming")) }

        assertContains(error.message.orEmpty(), "only core:data (as OfflineFirst<Interface>)")
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
    fun given_v1DeclarationShapeFixture_whenChecked_thenFailsOnNonNavKeyDeclaration() {
        val error = assertFails { checkV1FeatureApiShape(fixtureScope("v1-declaration-shape")) }

        assertContains(error.message.orEmpty(), "feature:x:api main files declare only NavKeys")
    }

    @Test
    fun given_v1MissingSerializerFixture_whenChecked_thenFailsOnMissingSerializerVal() {
        val error = assertFails { checkV1FeatureApiShape(fixtureScope("v1-missing-serializer")) }

        assertContains(error.message.orEmpty(), "must expose exactly one top-level val")
    }
}
