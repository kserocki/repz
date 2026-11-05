package com.repz.app.konsist.rules

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoAnnotationDeclaration
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndObjectDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import com.repz.app.konsist.support.repzSourceFile

private val KOIN_DEFINITION_ANNOTATIONS = listOf("Single", "Factory", "Scoped", "KoinViewModel")
private const val COMPONENT_SCAN_ANNOTATION = "ComponentScan"
private const val MODULE_ANNOTATION = "Module"
private const val APP_SHARED_MODULE_PATH = ":app:shared"

/** D1: every `@ComponentScan` names one Gradle module's root package, with no overlap (ARCH §12.2, §3.3). */
fun checkD1ComponentScanConfiguredPerModule(scope: KoScope) {
    val componentScans =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName(COMPONENT_SCAN_ANNOTATION) && !it.repzSourceFile().isTest }

    componentScans.assertTrue(
        additionalMessage = "D1: @ComponentScan takes exactly one non-empty string argument naming the module's root package (ARCH §12.2)",
    ) { classOrObject ->
        val annotation = componentScanAnnotationOf(classOrObject)
        annotation != null && annotation.numArguments == 1 &&
            !annotation.arguments
                .single()
                .value
                .isNullOrBlank()
    }

    componentScans
        .filter { it.repzSourceFile().modulePath != APP_SHARED_MODULE_PATH }
        .assertTrue(
            additionalMessage = "D1: outside app:shared, @ComponentScan sits on the module's one @Module class (ARCH §12.2)",
        ) { it.hasAnnotationWithName(MODULE_ANNOTATION) }

    val scanPackagesByModule = componentScanPackagesByModule(componentScans)
    val overlapping =
        scanPackagesByModule.entries
            .flatMap { (modulePath, scanPackages) -> scanPackages.map { modulePath to it } }
            .filter { (modulePath, scanPackage) ->
                scanPackagesByModule.any { (otherModulePath, otherScanPackages) ->
                    otherModulePath != modulePath && otherScanPackages.any { segmentsOverlap(scanPackage, it) }
                }
            }

    check(overlapping.isEmpty()) {
        "D1: no two Gradle modules' @ComponentScan packages may be equal or segment-prefixes of each other (ARCH §12.2): " +
            overlapping.joinToString { (modulePath, scanPackage) -> "$modulePath -> $scanPackage" }
    }
}

/**
 * D2: every class-level `@Single`/`@Factory`/`@Scoped`/`@KoinViewModel` (main source sets) lies
 * inside a `@ComponentScan` package of its own Gradle module, or it is never registered;
 * `@Singleton` is forbidden everywhere (ADR-0008, knowledge base).
 */
fun checkD2AnnotatedClassesInsideOwnModuleScan(scope: KoScope) {
    val scanPackagesByModule =
        componentScanPackagesByModule(scope.classesAndObjects().filter { it.hasAnnotationWithName(COMPONENT_SCAN_ANNOTATION) })

    scope
        .classesAndObjects()
        .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) && !it.repzSourceFile().isTest }
        .assertTrue(
            additionalMessage =
                "D2: a class-level Koin definition must lie inside its own module's @ComponentScan package, " +
                    "or it is never registered (ARCH §12.2)",
        ) { classOrObject ->
            val packageName = classOrObject.packagee?.name
            val ownScanPackages = scanPackagesByModule[classOrObject.repzSourceFile().modulePath].orEmpty()
            packageName != null && ownScanPackages.any { matchesScanPackage(packageName, it) }
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
            .filter { it.hasAnnotationWithName(MODULE_ANNOTATION) && !it.repzSourceFile().isTest }
            .groupBy { it.repzSourceFile().modulePath }

    moduleClassesByModule
        .filterKeys { it != APP_SHARED_MODULE_PATH }
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
        .filter { it.hasAnnotationWithName(MODULE_ANNOTATION) }
        .flatMap { it.functions(includeNested = false, includeLocal = false) }
        .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) }
        .assertTrue(additionalMessage = "D4: Koin provider functions declare an explicit return type (ARCH §9)") {
            it.hasReturnType()
        }
}

/**
 * D5: each Koin definition - a provider function inside a `@Module` class, or a `@Single`/
 * `@Factory`/`@Scoped`/`@KoinViewModel` class - has exactly one owner: the pair of (bound type,
 * qualifier annotations) is unique across every main source set (ARCH §9, §12.2). A class-level
 * definition contributes its own name plus every type it auto-binds to: the explicit `binds` list
 * if present, else every direct parent name except `Any`, `ViewModel`, `AndroidViewModel`,
 * `KoinComponent`, `KoinScopeComponent` (mirrors the koin-compiler-plugin's `detectAutoBindings`;
 * including a non-interface parent can only over-report, never hide a real collision). The one
 * exception is a binding provided once per platform inside an `expect`/`actual` `@Module`
 * (ADR-0008's S1 pattern, e.g. `PlatformModule.secureStringStorage`): every `actual` with the same
 * fully qualified class name counts as one owner, since `androidMain`/`iosMain` (and their test
 * source sets) never compile into the same graph — but two such `actual`s in the *same* source
 * set, or two differently named classes, still count as separate owners and still fail.
 */
fun checkD5OneOwnerPerDefinition(scope: KoScope) {
    data class Definition(
        val boundType: String,
        val qualifier: String,
        val location: String,
        val owner: KoinOwner,
    )

    val fromProviderFunctions =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName(MODULE_ANNOTATION) }
            .flatMap { moduleClass -> moduleClass.functions(includeNested = false, includeLocal = false).map { it to moduleClass } }
            .filter { (function, _) -> function.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) && !function.repzSourceFile().isTest }
            .mapNotNull { (function, moduleClass) ->
                val returnType = function.returnType?.sourceType ?: return@mapNotNull null
                Definition(
                    boundType = returnType,
                    qualifier = qualifierKey(function.annotations),
                    location = "${function.repzSourceFile().modulePath}.${function.name}()",
                    owner = koinOwnerOf(moduleClass),
                )
            }

    val fromDefinitionClasses =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) && !it.repzSourceFile().isTest }
            .flatMap { classOrObject ->
                val definitionAnnotation = classOrObject.annotations.first { it.name in KOIN_DEFINITION_ANNOTATIONS }
                val qualifier = qualifierKey(classOrObject.annotations)
                val location = "${classOrObject.repzSourceFile().modulePath}.${classOrObject.name}"
                val owner = koinOwnerOf(classOrObject)
                boundTypeNamesOf(classOrObject, definitionAnnotation).map { boundType ->
                    Definition(boundType = boundType, qualifier = qualifier, location = location, owner = owner)
                }
            }

    val duplicateOwners =
        (fromProviderFunctions + fromDefinitionClasses)
            .groupBy { it.boundType to it.qualifier }
            .filterValues { it.size > 1 }
            .filterValues { definitions -> !isOnePerPlatformActual(definitions.map { it.owner }) }

    check(duplicateOwners.isEmpty()) {
        "D5: each Koin definition has exactly one owner (ARCH §9): " +
            duplicateOwners.entries.joinToString { (key, definitions) -> "${key.first} -> ${definitions.map { it.location }}" }
    }
}

/**
 * D6: no pass-through provider (ADR-0008). A provider function inside a `@Module` class, in
 * `commonMain` (scanning `androidMain`/`iosMain` from a `commonMain` `@ComponentScan` is
 * unverified and out of scope for this migration, so `PlatformModule`'s platform `actual`
 * providers are not evaluated here), whose entire body is a single constructor call of a class
 * declared in this project, outside `core:domain`/`core:common`, with every argument a bare
 * identifier (not computed, e.g. `database.currentUserDao()` or `Clock.System`), does nothing a
 * class-level annotation couldn't: annotate that class instead. `core:testing`'s cross-module
 * probe is exempt: its whole purpose is proving a provider-function definition still resolves
 * correctly next to a scanned one, so it deliberately keeps one even though it is a bare
 * pass-through.
 */
fun checkD6NoPassThroughProvider(scope: KoScope) {
    val classModulePaths = scope.classes().groupBy({ it.name }, { it.repzSourceFile().modulePath })

    val violations =
        scope
            .classesAndObjects()
            .filter { it.hasAnnotationWithName(MODULE_ANNOTATION) }
            .flatMap { it.functions(includeNested = false, includeLocal = false) }
            .filter { function ->
                function.hasAnnotationWithName(KOIN_DEFINITION_ANNOTATIONS) &&
                    function.repzSourceFile().sourceSetName == "commonMain" &&
                    function.repzSourceFile().modulePath !in PASS_THROUGH_EXEMPT_PROVIDER_MODULE_PATHS
            }.mapNotNull { function ->
                val calleeName = passThroughCalleeName(function) ?: return@mapNotNull null
                val calleeModulePaths = classModulePaths[calleeName].orEmpty()
                if (calleeModulePaths.isEmpty() || calleeModulePaths.all { it in PASS_THROUGH_EXEMPT_CALLEE_MODULE_PATHS }) {
                    return@mapNotNull null
                }
                "${function.repzSourceFile().modulePath}.${function.name}() -> annotate $calleeName instead (ADR-0008)"
            }

    check(violations.isEmpty()) { "D6: no pass-through providers (ADR-0008): ${violations.joinToString()}" }
}

private val PASS_THROUGH_EXEMPT_CALLEE_MODULE_PATHS = setOf(":core:domain", ":core:common")
private val PASS_THROUGH_EXEMPT_PROVIDER_MODULE_PATHS = setOf(":core:testing")

/**
 * D7: a class's name states its role, so its Koin definition kind must match it (ARCH §12.2):
 * `*UseCase` outside `core:domain` (which is Koin-free, ADR-0008) is `@Factory`, `*ViewModel` is
 * `@KoinViewModel`, `OfflineFirst*Repository` is `@Single`.
 */
fun checkD7DefinitionKindMatchesRole(scope: KoScope) {
    val classesOutsideDomain = scope.classes().filter { !it.repzSourceFile().isTest && it.repzSourceFile().modulePath != ":core:domain" }

    ROLE_ANNOTATION_RULES.forEach { rule ->
        classesOutsideDomain
            .filter { rule.nameMatches(it.name) }
            .assertTrue(additionalMessage = "D7: ${rule.roleLabel} classes are @${rule.expectedAnnotation} (ARCH §12.2)") {
                it.hasAnnotationWithName(rule.expectedAnnotation)
            }
    }
}

private data class RoleAnnotationRule(
    val roleLabel: String,
    val nameMatches: (String) -> Boolean,
    val expectedAnnotation: String,
)

private val ROLE_ANNOTATION_RULES =
    listOf(
        RoleAnnotationRule("*UseCase", { it.endsWith("UseCase") }, "Factory"),
        RoleAnnotationRule("*ViewModel", { it.endsWith("ViewModel") }, "KoinViewModel"),
        RoleAnnotationRule("OfflineFirst*Repository", { it.startsWith("OfflineFirst") && it.endsWith("Repository") }, "Single"),
    )

// --- D1/D2 scan-package helpers -------------------------------------------------------------

private fun componentScanAnnotationOf(classOrObject: KoClassAndObjectDeclaration): KoAnnotationDeclaration? =
    classOrObject.annotations.firstOrNull { it.name == COMPONENT_SCAN_ANNOTATION }

private fun componentScanPackageValueOf(classOrObject: KoClassAndObjectDeclaration): String? =
    componentScanAnnotationOf(classOrObject)
        ?.arguments
        ?.singleOrNull()
        ?.value
        ?.takeIf { it.isNotBlank() }

/** Every `@ComponentScan` package string found in [componentScans], grouped by its own Gradle module. */
private fun componentScanPackagesByModule(componentScans: List<KoClassAndObjectDeclaration>): Map<String, List<String>> =
    componentScans
        .mapNotNull { classOrObject -> componentScanPackageValueOf(classOrObject)?.let { classOrObject.repzSourceFile().modulePath to it } }
        .groupBy({ it.first }, { it.second })

/** Mirrors the koin-compiler-plugin's `matchesScanPackages`: a package matches a scan at or below it, never a sibling. */
private fun matchesScanPackage(
    packageName: String,
    scanPackage: String,
): Boolean = packageName == scanPackage || packageName.startsWith("$scanPackage.")

/** `true` when [a] and [b], split on `.`, are equal or one is a segment-prefix of the other. */
private fun segmentsOverlap(
    a: String,
    b: String,
): Boolean {
    val segmentsA = a.split(".")
    val segmentsB = b.split(".")
    return segmentsA.take(segmentsB.size) == segmentsB || segmentsB.take(segmentsA.size) == segmentsA
}

// --- D5 auto-binding helpers -----------------------------------------------------------------

// Mirrors the koin-compiler-plugin's `KoinPluginConstants.AUTO_BIND_EXCLUDED_SUPERTYPES` (1.0.2 behaviour, ARCH §12.2 Q1).
private val AUTO_BIND_EXCLUDED_SUPERTYPES = setOf("Any", "ViewModel", "AndroidViewModel", "KoinComponent", "KoinScopeComponent")
private val BINDS_CLASS_REFERENCE = Regex("""([A-Za-z_][A-Za-zA-Z0-9_]*)\s*::\s*class""")

/** The class's own name plus every type it binds to: the explicit `binds` list if present, else its auto-bound parents. */
private fun boundTypeNamesOf(
    classOrObject: KoClassAndObjectDeclaration,
    definitionAnnotation: KoAnnotationDeclaration,
): Set<String> {
    val boundSupertypes = explicitBindsClassNames(definitionAnnotation) ?: autoBoundParentNames(classOrObject)
    return setOf(classOrObject.name) + boundSupertypes
}

/** `null` when there is no explicit `binds = [...]` argument (fall back to auto-binding); the parsed class names otherwise. */
private fun explicitBindsClassNames(annotation: KoAnnotationDeclaration): List<String>? {
    val bindsValue = annotation.arguments.firstOrNull { it.name == "binds" }?.value ?: return null
    return BINDS_CLASS_REFERENCE.findAll(bindsValue).map { it.groupValues[1] }.toList()
}

private fun autoBoundParentNames(classOrObject: KoClassAndObjectDeclaration): List<String> =
    classOrObject.parents(indirectParents = false).map { it.name }.filterNot { it in AUTO_BIND_EXCLUDED_SUPERTYPES }

/**
 * True when every owner sharing a (bound type, qualifier) is an `actual` class with the same
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
private val NON_QUALIFIER_ANNOTATIONS = KOIN_DEFINITION_ANNOTATIONS + listOf(MODULE_ANNOTATION, "OptIn", "Suppress", "JvmStatic")

private fun qualifierKey(annotations: List<KoAnnotationDeclaration>): String =
    annotations
        .filterNot { it.name in NON_QUALIFIER_ANNOTATIONS }
        .map { it.text }
        .sorted()
        .joinToString(",")

// --- D6 pass-through-provider helpers ---------------------------------------------------------

private val CONSTRUCTOR_CALL_START = Regex("""^([A-Za-z_][A-Za-zA-Z0-9_]*)\(""")
private val NAMED_ARGUMENT_LABEL = Regex("""^[A-Za-z_][A-Za-zA-Z0-9_]*\s*=(?!=)\s*""")
private val BARE_IDENTIFIER = Regex("""^[A-Za-z_][A-Za-zA-Z0-9_]*[?!]{0,2}$""")

/**
 * The name of the project class constructed by [function]'s body, when that body is exactly one
 * call `X(arg1, arg2, …)` with every argument a bare identifier (an unlabelled or
 * `name = value`-labelled parameter forward); `null` otherwise, including when the call is
 * qualified (`a.b(...)`, e.g. `database.currentUserDao()`) or any argument is itself computed
 * (e.g. `Logger.withTag(TAG)`, `platformDefaultApiBaseUrl()`).
 */
private fun passThroughCalleeName(function: KoFunctionDeclaration): String? {
    val body = providerFunctionBodyText(function) ?: return null
    return calleeAndArgumentsOf(body)?.let { (calleeName, arguments) -> calleeName.takeIf { arguments.all(::isSimpleArgument) } }
}

/** The whole-body call's callee name and its top-level arguments, or `null` if the body isn't exactly one call spanning it. */
private fun calleeAndArgumentsOf(body: String): Pair<String, List<String>>? {
    val calleeMatch = CONSTRUCTOR_CALL_START.find(body)
    val openIndex = calleeMatch?.range?.last

    return openIndex?.let { index ->
        matchingCloseIndex(body, index, '(', ')')
            ?.takeIf { it == body.length - 1 }
            ?.let { closeIndex -> calleeMatch.groupValues[1] to splitTopLevelArguments(body.substring(index + 1, closeIndex)) }
    }
}

/** The function's body as source text: the expression after `=`, or the single statement inside `{ }` with `return` stripped. */
private fun providerFunctionBodyText(function: KoFunctionDeclaration): String? {
    val text = function.text
    val signatureEnd = Regex("""\bfun\s+${Regex.escape(function.name)}\b""").find(text)?.range?.last
    val paramsOpenIndex = signatureEnd?.let { text.indexOf('(', it) }?.takeIf { it >= 0 }

    return paramsOpenIndex?.let { openIndex ->
        matchingCloseIndex(text, openIndex, '(', ')')?.let { paramsCloseIndex ->
            bodyTextAfterParameterList(text.substring(paramsCloseIndex + 1))
        }
    }
}

/** `afterParams` is everything after the parameter list's closing `)`: an optional return type, then `= expr` or `{ … }`. */
private fun bodyTextAfterParameterList(afterParams: String): String? {
    val equalsIndex = afterParams.indexOf('=')
    val braceIndex = afterParams.indexOf('{')

    return when {
        equalsIndex >= 0 && (braceIndex < 0 || equalsIndex < braceIndex) -> {
            afterParams.substring(equalsIndex + 1).trim()
        }

        braceIndex >= 0 -> {
            matchingCloseIndex(afterParams, braceIndex, '{', '}')?.let { braceCloseIndex ->
                afterParams
                    .substring(braceIndex + 1, braceCloseIndex)
                    .trim()
                    .removePrefix("return")
                    .trim()
            }
        }

        else -> {
            null
        }
    }
}

/** The index of the `close` that balances the `open` at [openIndex] (depth-aware, so nested pairs don't confuse it). */
private fun matchingCloseIndex(
    text: String,
    openIndex: Int,
    open: Char,
    close: Char,
): Int? {
    var depth = 0
    for (index in openIndex until text.length) {
        when (text[index]) {
            open -> {
                depth++
            }

            close -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

/** Splits a call's argument list on its top-level commas, so a nested call's own commas are kept inside one argument. */
private fun splitTopLevelArguments(argumentsText: String): List<String> {
    if (argumentsText.isBlank()) return emptyList()

    val arguments = mutableListOf<String>()
    var depth = 0
    var segmentStart = 0
    argumentsText.forEachIndexed { index, char ->
        when (char) {
            '(', '[', '{' -> {
                depth++
            }

            ')', ']', '}' -> {
                depth--
            }

            ',' -> {
                if (depth == 0) {
                    arguments += argumentsText.substring(segmentStart, index)
                    segmentStart = index + 1
                }
            }
        }
    }
    arguments += argumentsText.substring(segmentStart)
    return arguments.map { it.trim() }.filter { it.isNotEmpty() }
}

private fun isSimpleArgument(argument: String): Boolean = BARE_IDENTIFIER.matches(argument.replaceFirst(NAMED_ARGUMENT_LABEL, "").trim())
