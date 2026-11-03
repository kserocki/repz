package com.repz.app.konsist.rules

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoAnnotationDeclaration
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import com.repz.app.konsist.support.repzSourceFile

private val KOIN_DEFINITION_ANNOTATIONS = listOf("Single", "Factory", "Scoped", "KoinViewModel")

/** D1: `@ComponentScan` is used only in `app:shared`, the one module allowed to auto-discover definitions (ADR-0008 option A). */
fun checkD1ComponentScanOnlyInAppShared(scope: KoScope) {
    scope
        .classesAndObjects()
        .filter { it.hasAnnotationWithName("ComponentScan") }
        .assertTrue(additionalMessage = "D1: @ComponentScan is used only in app:shared (ADR-0008)") {
            it.repzSourceFile().modulePath == ":app:shared"
        }
}

/**
 * D2: `@Single`/`@Factory`/`@Scoped`/`@KoinViewModel` classes live only in `app:shared`;
 * `@Singleton` is forbidden everywhere (ADR-0008, knowledge base).
 */
fun checkD2KoinDefinitionClassesOnlyInAppShared(scope: KoScope) {
    scope
        .classesAndObjects()
        .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) }
        .assertTrue(additionalMessage = "D2: @Single/@Factory/@Scoped/@KoinViewModel classes live only in app:shared (ADR-0008)") {
            it.repzSourceFile().modulePath == ":app:shared"
        }

    scope
        .classesAndObjects()
        .assertFalse(additionalMessage = "D2: @Singleton is forbidden, use @Single instead (ADR-0008)") {
            it.hasAnnotationWithName("Singleton")
        }
}

/** D3: every module except `app:shared` has at most one `@Module` class in its main source sets (ARCH §9). */
fun checkD3AtMostOneModuleClassPerModule(scope: KoScope) {
    val moduleClassesByModule =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName("Module") && !it.repzSourceFile().isTest }
            .groupBy { it.repzSourceFile().modulePath }

    moduleClassesByModule
        .filterKeys { it != ":app:shared" }
        .forEach { (modulePath, moduleClasses) ->
            check(moduleClasses.size <= 1) {
                "D3: $modulePath has ${moduleClasses.size} @Module classes in main source sets, at most one is allowed " +
                    "(ARCH §9): ${moduleClasses.joinToString { it.name }}"
            }
        }
}

/**
 * D4: provider functions (`@Single`/`@Factory`/`@Scoped`/`@KoinViewModel` functions inside a
 * `@Module` class) declare an explicit return type, needed for D5 (ARCH §9).
 */
fun checkD4ProviderFunctionsDeclareReturnType(scope: KoScope) {
    scope
        .classesAndObjects()
        .filter { it.hasAnnotationWithName("Module") }
        .flatMap { it.functions(includeNested = false, includeLocal = false) }
        .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) }
        .assertTrue(additionalMessage = "D4: Koin provider functions declare an explicit return type (ARCH §9)") {
            it.hasReturnType()
        }
}

/**
 * D5: each Koin definition - a provider function inside a `@Module` class, or a `@Single`/
 * `@Factory`/`@Scoped`/`@KoinViewModel` class - has exactly one owner: the pair of (return type,
 * qualifier annotations) is unique across every main source set (ARCH §9, §12.2). The one
 * exception is a binding provided once per platform inside an `expect`/`actual` `@Module`
 * (ADR-0008's S1 pattern, e.g. `PlatformModule.secureStringStorage`): every `actual` with the same
 * fully qualified class name counts as one owner, since `androidMain`/`iosMain` (and their test
 * source sets) never compile into the same graph — but two such `actual`s in the *same* source
 * set, or two differently named classes, still count as separate owners and still fail.
 */
fun checkD5OneOwnerPerDefinition(scope: KoScope) {
    data class Definition(
        val returnType: String,
        val qualifier: String,
        val location: String,
        val owner: KoinOwner,
    )

    val fromProviderFunctions =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName("Module") }
            .flatMap { moduleClass -> moduleClass.functions(includeNested = false, includeLocal = false).map { it to moduleClass } }
            .filter { (function, _) -> function.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) && !function.repzSourceFile().isTest }
            .mapNotNull { (function, moduleClass) ->
                val returnType = function.returnType?.sourceType ?: return@mapNotNull null
                Definition(
                    returnType = returnType,
                    qualifier = qualifierKey(function.annotations),
                    location = "${function.repzSourceFile().modulePath}.${function.name}()",
                    owner = koinOwnerOf(moduleClass),
                )
            }

    val fromDefinitionClasses =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) && !it.repzSourceFile().isTest }
            .map { classOrObject ->
                Definition(
                    returnType = classOrObject.name,
                    qualifier = qualifierKey(classOrObject.annotations),
                    location = "${classOrObject.repzSourceFile().modulePath}.${classOrObject.name}",
                    owner = koinOwnerOf(classOrObject),
                )
            }

    val duplicateOwners =
        (fromProviderFunctions + fromDefinitionClasses)
            .groupBy { it.returnType to it.qualifier }
            .filterValues { it.size > 1 }
            .filterValues { definitions -> !isOnePerPlatformActual(definitions.map { it.owner }) }

    check(duplicateOwners.isEmpty()) {
        "D5: each Koin definition has exactly one owner (ARCH §9): " +
            duplicateOwners.entries.joinToString { (key, definitions) -> "${key.first} -> ${definitions.map { it.location }}" }
    }
}

/**
 * True when every owner sharing a (return type, qualifier) is an `actual` class with the same
 * fully qualified name, each in a distinct source set (so no two can ever compile together).
 */
private fun isOnePerPlatformActual(owners: List<KoinOwner>): Boolean {
    val allActual = owners.all { it.isActual }
    val fullyQualifiedNames = owners.mapTo(mutableSetOf()) { it.fullyQualifiedName }
    val sameNonNullClass = fullyQualifiedNames.size == 1 && fullyQualifiedNames.single() != null
    val sourceSetNames = owners.map { it.sourceSetName }
    val onePerSourceSet = sourceSetNames.toSet().size == sourceSetNames.size

    return allActual && sameNonNullClass && onePerSourceSet
}

private data class KoinOwner(
    val isActual: Boolean,
    val fullyQualifiedName: String?,
    val sourceSetName: String?,
)

private fun koinOwnerOf(classOrObject: KoClassAndObjectDeclaration): KoinOwner =
    KoinOwner(
        isActual = (classOrObject as? KoClassDeclaration)?.hasActualModifier == true,
        fullyQualifiedName = classOrObject.fullyQualifiedName,
        sourceSetName = classOrObject.repzSourceFile().sourceSetName,
    )

// "Module" (class-level) plus annotations unrelated to Koin qualification: without these, two
// definitions that differ only by @OptIn/@Suppress/@JvmStatic would wrongly count as distinct.
private val NON_QUALIFIER_ANNOTATIONS = KOIN_DEFINITION_ANNOTATIONS + listOf("Module", "OptIn", "Suppress", "JvmStatic")

private fun qualifierKey(annotations: List<KoAnnotationDeclaration>): String =
    annotations
        .filterNot { it.name in NON_QUALIFIER_ANNOTATIONS }
        .map { it.text }
        .sorted()
        .joinToString(",")
