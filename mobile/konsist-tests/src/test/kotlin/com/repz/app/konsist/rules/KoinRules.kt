package com.repz.app.konsist.rules

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoAnnotationDeclaration
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

/** D2: `@Single`/`@Factory`/`@Scoped`/`@KoinViewModel` classes live only in `app:shared`; `@Singleton` is forbidden everywhere (ADR-0008, knowledge base). */
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

/** D4: provider functions (`@Single`/`@Factory`/`@Scoped`/`@KoinViewModel` functions inside a `@Module` class) declare an explicit return type, needed for D5 (ARCH §9). */
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
 * qualifier annotations) is unique across every main source set (ARCH §9, REP-7 user decision).
 * The return type is compared by its source text, not a resolved fully qualified name (Konsist does
 * not do type resolution here by design, D3 in the plan); documented as a heuristic in ARCH §12.
 */
fun checkD5OneOwnerPerDefinition(scope: KoScope) {
    data class Definition(val returnType: String, val qualifier: String, val location: String)

    val fromProviderFunctions =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName("Module") }
            .flatMap { it.functions(includeNested = false, includeLocal = false) }
            .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) && !it.repzSourceFile().isTest }
            .mapNotNull { function ->
                val returnType = function.returnType?.sourceType ?: return@mapNotNull null
                Definition(returnType, qualifierKey(function.annotations), "${function.repzSourceFile().modulePath}.${function.name}()")
            }

    val fromDefinitionClasses =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) && !it.repzSourceFile().isTest }
            .map { classOrObject ->
                Definition(
                    classOrObject.name,
                    qualifierKey(classOrObject.annotations),
                    "${classOrObject.repzSourceFile().modulePath}.${classOrObject.name}",
                )
            }

    val duplicateOwners =
        (fromProviderFunctions + fromDefinitionClasses)
            .groupBy { it.returnType to it.qualifier }
            .filterValues { it.size > 1 }

    check(duplicateOwners.isEmpty()) {
        "D5: each Koin definition has exactly one owner (ARCH §9): " +
            duplicateOwners.entries.joinToString { (key, definitions) -> "${key.first} -> ${definitions.map { it.location }}" }
    }
}

private fun qualifierKey(annotations: List<KoAnnotationDeclaration>): String =
    annotations.map { it.name }.filterNot { it in KOIN_DEFINITION_ANNOTATIONS }.sorted().joinToString(",")
