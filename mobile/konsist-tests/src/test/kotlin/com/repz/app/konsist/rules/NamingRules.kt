package com.repz.app.konsist.rules

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import com.repz.app.konsist.support.repzSourceFile

private val FEATURE_IMPL_MODULE = Regex(""":feature:[^:]+:impl""")

/**
 * N1: a class extending `ViewModel` is named `*ViewModel` and vice versa; it lives in a
 * `..presentation..` package of a `feature:x:impl`; it has `XUiState`/`XIntent`/`XEffect` in the
 * same package, and exposes `state`, `effects` and `fun onIntent(intent: XIntent)` (AGENTS.md §4,
 * ADR-0005, new-feature skill).
 */
fun checkN1ViewModelShapeAndNaming(scope: KoScope) {
    val classesAndInterfaces = scope.classesAndInterfaces()

    classesAndInterfaces
        .filter { it.hasParentWithName("ViewModel") }
        .assertTrue(additionalMessage = "N1: a class extending ViewModel is named *ViewModel (AGENTS.md §4, ADR-0005)") {
            it.name.endsWith("ViewModel")
        }

    classesAndInterfaces
        .filter { it.name.endsWith("ViewModel") && !it.repzSourceFile().isTest }
        .assertTrue(additionalMessage = "N1: a class named *ViewModel extends ViewModel (AGENTS.md §4, ADR-0005)") {
            it.hasParentWithName("ViewModel")
        }

    val viewModels = scope.classes().filter { it.hasParentWithName("ViewModel") }

    viewModels.assertTrue(additionalMessage = "N1: a ViewModel lives in a ..presentation.. package of a feature:x:impl (ADR-0005)") {
        it.resideInPackage("..presentation..") && FEATURE_IMPL_MODULE.matches(it.repzSourceFile().modulePath)
    }

    viewModels.assertTrue(
        additionalMessage = "N1: XViewModel exposes state, effects and fun onIntent(intent: XIntent) (AGENTS.md §4)",
    ) { viewModel ->
        viewModel.hasPropertyWithName("state") && viewModel.hasPropertyWithName("effects") && viewModel.hasFunctionWithName("onIntent")
    }

    viewModels.assertTrue(
        additionalMessage = "N1: XViewModel has XUiState/XIntent/XEffect in the same package (AGENTS.md §4)",
    ) { viewModel ->
        val baseName = viewModel.name.removeSuffix("ViewModel")
        val siblingsInPackage = classesAndInterfaces.filter { it.packagee?.name == viewModel.packagee?.name }
        siblingsInPackage.any { it.name == "${baseName}UiState" } &&
            siblingsInPackage.any { it.name == "${baseName}Intent" } &&
            siblingsInPackage.any { it.name == "${baseName}Effect" }
    }
}

private val FORBIDDEN_UI_STATE_COLLECTION_TYPES = listOf("List", "Set", "Map", "MutableList", "MutableSet", "MutableMap")

/** N2: `*UiState` properties are never `List`/`Set`/`Map` (`ImmutableList`/`ImmutableSet`/`ImmutableMap` instead, AGENTS.md §4). */
fun checkN2UiStatePropertiesAreImmutableCollections(scope: KoScope) {
    scope
        .classes()
        .filter { it.name.endsWith("UiState") }
        .flatMap { it.properties(includeNested = false) }
        .assertTrue(
            additionalMessage = "N2: *UiState properties use ImmutableList/ImmutableSet/ImmutableMap, never List/Set/Map (AGENTS.md §4)",
        ) { property ->
            FORBIDDEN_UI_STATE_COLLECTION_TYPES.none { property.hasTacitType(it) }
        }
}

/**
 * N3: `*UseCase` classes live in `core:domain` or a `..domain..` package of a `feature:x:impl`,
 * and declare exactly one non-private function: `operator fun invoke` (AGENTS.md §4).
 */
fun checkN3UseCaseShape(scope: KoScope) {
    val useCases = scope.classes().filter { it.name.endsWith("UseCase") }

    useCases.assertTrue(
        additionalMessage = "N3: *UseCase classes live in core:domain or a ..domain.. package of feature:x:impl (AGENTS.md §4)",
    ) {
        it.repzSourceFile().modulePath == ":core:domain" ||
            (FEATURE_IMPL_MODULE.matches(it.repzSourceFile().modulePath) && it.resideInPackage("..domain.."))
    }

    useCases.assertTrue(
        additionalMessage = "N3: *UseCase declares exactly one non-private function: operator fun invoke (AGENTS.md §4)",
    ) { useCase ->
        val nonPrivateFunctions = useCase.functions(includeNested = false, includeLocal = false).filterNot { it.hasPrivateModifier }
        val onlyFunction = nonPrivateFunctions.singleOrNull()
        onlyFunction != null && onlyFunction.name == "invoke" && onlyFunction.hasModifier(KoModifier.OPERATOR)
    }
}

/**
 * N4: a `@Composable fun XScreen` has a matching `@Composable fun XContent` in the same package;
 * `XContent` takes `state` and `onIntent`, never a `*ViewModel` parameter, and never calls
 * `koinViewModel(`; `koinViewModel(` is called only inside `*Screen` functions; at least one
 * `@Preview` function in the same file calls `XContent(` (AGENTS.md §4, ADR-0005).
 */
fun checkN4ScreenContentPairing(scope: KoScope) {
    val composableFunctions = scope.functions().filter { it.hasAnnotationWithName("Composable") }
    val screenFunctions = composableFunctions.filter { it.name.endsWith("Screen") }
    val contentFunctions = composableFunctions.filter { it.name.endsWith("Content") }

    screenFunctions.assertTrue(additionalMessage = "N4: XScreen has a matching XContent in the same package (ADR-0005)") { screen ->
        val baseName = screen.name.removeSuffix("Screen")
        contentFunctions.any { it.name == "${baseName}Content" && it.packagee?.name == screen.packagee?.name }
    }

    contentFunctions.assertTrue(
        additionalMessage = "N4: XContent takes state and onIntent, never a ViewModel parameter, and never calls koinViewModel( (ADR-0005)",
    ) { content ->
        content.hasParameterWithName("state") &&
            content.hasParameterWithName("onIntent") &&
            content.parameters.none { it.type.name.endsWith("ViewModel") } &&
            "koinViewModel(" !in content.text
    }

    composableFunctions.assertTrue(
        additionalMessage = "N4: koinViewModel( is called only inside *Screen functions (ADR-0005)",
    ) { function ->
        "koinViewModel(" !in function.text || function.name.endsWith("Screen")
    }

    screenFunctions.assertTrue(
        additionalMessage = "N4: at least one @Preview function in the same file calls XContent( (ADR-0005)",
    ) { screen ->
        val baseName = screen.name.removeSuffix("Screen")
        val previewsInFile =
            composableFunctions.filter {
                it.hasAnnotationWithName("Preview") &&
                    it.repzSourceFile() == screen.repzSourceFile()
            }
        previewsInFile.any { "${baseName}Content(" in it.text }
    }
}

/**
 * N5: `*Repository` interfaces live in `core:domain`; a `core:data` implementation is named
 * `OfflineFirst<Interface>`; outside `core:data` only `core:testing` and test source sets may
 * implement one (AGENTS.md §4).
 */
fun checkN5RepositoryShape(scope: KoScope) {
    val repositoryInterfaces = scope.interfaces().filter { it.name.endsWith("Repository") }

    repositoryInterfaces.assertTrue(additionalMessage = "N5: *Repository interfaces live in core:domain (AGENTS.md §4)") {
        it.repzSourceFile().modulePath == ":core:domain"
    }

    val repositoryNames = repositoryInterfaces.map { it.name }.toSet()

    scope
        .classes()
        .filter { classDeclaration -> classDeclaration.parents().any { it.name in repositoryNames } }
        .assertTrue(
            additionalMessage = "N5: only core:data (as OfflineFirst<Interface>), core:testing or test source sets implement a *Repository",
        ) { implementation ->
            val source = implementation.repzSourceFile()
            when {
                source.isTest -> {
                    true
                }

                source.modulePath == ":core:testing" -> {
                    true
                }

                source.modulePath == ":core:data" -> {
                    val repositoryParentName = implementation.parents().first { it.name in repositoryNames }.name
                    implementation.name == "OfflineFirst$repositoryParentName"
                }

                else -> {
                    false
                }
            }
        }
}

/** N6: `@Test` functions match `^given.+_when.+_then.+$` (AGENTS.md §4). */
fun checkN6TestFunctionNaming(scope: KoScope) {
    val givenWhenThen = Regex("^given.+_when.+_then.+$")

    scope
        .functions()
        .filter { it.hasAnnotationWithName("Test") }
        .assertTrue(
            additionalMessage = "N6: @Test functions are named given…_when…_then… (AGENTS.md §4)",
        ) { givenWhenThen.matches(it.name) }
}

/** N7: no `io.mockk`/`org.mockito` imports - hand-written fakes only (AGENTS.md §4). */
fun checkN7NoMockingLibraries(scope: KoScope) {
    scope.imports.assertFalse(additionalMessage = "N7: no mocking libraries, hand-written fakes only (AGENTS.md §4)") {
        it.name.startsWith("io.mockk") || it.name.startsWith("org.mockito")
    }
}
