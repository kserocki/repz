package com.repz.app.konsist.rules

import com.repz.app.konsist.support.RepzTextFile
import com.repz.app.konsist.support.assertTrue
import com.repz.app.konsist.support.hasImportStartingWith
import com.repz.app.konsist.support.importedNames

private val FEATURE_IMPL_MODULE = Regex(""":feature:[^:]+:impl""")
private val FEATURE_API_MODULE = Regex(""":feature:[^:]+:api""")
private val FEATURE_IMPL_IMPORT = Regex("""com\.repz\.app\.feature\.([a-zA-Z0-9]+)\.impl""")

private val A1_CORE_DOMAIN_FORBIDDEN_IMPORTS = listOf("android.", "androidx.", "io.ktor.", "org.koin.", "org.jetbrains.compose.")
private val A1_CORE_COMMON_FORBIDDEN_IMPORTS = listOf("org.koin.")
private val A1_FORBIDDEN_BUILD_FILE_TOKENS =
    listOf("repz.koin", "repz.compose", "repz.room", "libs.koin", "libs.ktor", "libs.androidx", "libs.compose")

/**
 * A1: `core:domain` is pure Kotlin (no Room, Ktor, Compose, Koin or `androidx.*`); `core:common`
 * never imports Koin; neither module's build file applies those plugins (AGENTS.md §3.1, ARCH §9).
 */
fun checkA1DomainAndCommonStayPure(files: List<RepzTextFile>) {
    files
        .filter { !it.source.isBuildScript && it.source.modulePath == ":core:domain" }
        .assertTrue("A1: core:domain is pure Kotlin - no Room/Ktor/Compose/Koin/androidx imports (AGENTS.md §3.1)") { file ->
            A1_CORE_DOMAIN_FORBIDDEN_IMPORTS.none { prefix -> hasImportStartingWith(file.text, prefix) }
        }

    files
        .filter { !it.source.isBuildScript && it.source.modulePath == ":core:common" }
        .assertTrue("A1: core:common never imports Koin (AGENTS.md §3.1)") { file ->
            A1_CORE_COMMON_FORBIDDEN_IMPORTS.none { prefix -> hasImportStartingWith(file.text, prefix) }
        }

    files
        .filter { it.source.isBuildScript && it.source.modulePath in setOf(":core:domain", ":core:common") }
        .assertTrue("A1: core:domain/core:common build files apply no Koin/Compose/Room plugin (AGENTS.md §3.1)") { file ->
            A1_FORBIDDEN_BUILD_FILE_TOKENS.none { token -> token in file.text }
        }
}

/**
 * A2: a `feature:x:impl` never imports another feature's `:impl`; a `feature:x:api` never imports
 * any feature's `:impl`; no feature build file depends on another feature's `:impl` project
 * (AGENTS.md §3.2).
 */
fun checkA2FeatureImplIsolation(files: List<RepzTextFile>) {
    files
        .filter { !it.source.isBuildScript && FEATURE_IMPL_MODULE.matches(it.source.modulePath) }
        .assertTrue("A2: feature:x:impl never imports another feature's :impl (AGENTS.md §3.2)") { file ->
            val ownFeatureName = file.source.modulePath.removePrefix(":feature:").removeSuffix(":impl")
            importedNames(file.text).none { name ->
                FEATURE_IMPL_IMPORT.find(name)?.groupValues?.get(1)?.let { it != ownFeatureName } == true
            }
        }

    files
        .filter { !it.source.isBuildScript && FEATURE_API_MODULE.matches(it.source.modulePath) }
        .assertTrue("A2: feature:x:api never imports a feature's :impl (AGENTS.md §3.2)") { file ->
            importedNames(file.text).none { FEATURE_IMPL_IMPORT.containsMatchIn(it) }
        }

    files
        .filter { it.source.isBuildScript && it.source.modulePath.startsWith(":feature:") }
        .assertTrue("A2: feature build files never depend on another feature's :impl (AGENTS.md §3.2)") { file ->
            !Regex("""projects\.feature\.\w+\.impl|:feature:\w+:impl""").containsMatchIn(file.text)
        }
}

private val A3_FORBIDDEN_IMPORTS =
    listOf("com.repz.app.core.data", "com.repz.app.core.database", "com.repz.app.core.network", "com.repz.app.core.sync")
private val A3_FORBIDDEN_BUILD_FILE_TOKENS =
    listOf(
        "projects.core.data", "projects.core.database", "projects.core.network", "projects.core.sync",
        ":core:data", ":core:database", ":core:network", ":core:sync",
    )

/** A3: `feature:x:impl` and `feature:x:api` see domain interfaces only, never `core:data`/`core:database`/`core:network`/`core:sync` (AGENTS.md §3.3). */
fun checkA3FeatureSeesDomainOnly(files: List<RepzTextFile>) {
    files
        .filter { !it.source.isBuildScript && (FEATURE_IMPL_MODULE.matches(it.source.modulePath) || FEATURE_API_MODULE.matches(it.source.modulePath)) }
        .assertTrue("A3: feature impl/api modules see domain interfaces only (AGENTS.md §3.3)") { file ->
            A3_FORBIDDEN_IMPORTS.none { forbidden -> hasImportStartingWith(file.text, forbidden) }
        }

    files
        .filter { it.source.isBuildScript && it.source.modulePath.startsWith(":feature:") }
        .assertTrue("A3: feature build files never depend on core:data/database/network/sync (AGENTS.md §3.3)") { file ->
            A3_FORBIDDEN_BUILD_FILE_TOKENS.none { token -> token in file.text }
        }
}

private val A4_COMPOSITION_ROOT_TOKENS = listOf("KoinApplication(", "startKoin(", "startKoin<", "NavDisplay(")

/** A4: `@KoinApplication`, `startKoin` and `NavDisplay` are used only in `app:shared`, the composition root; `core:*` never imports feature or `app:shared` code (AGENTS.md §3.4). */
fun checkA4CompositionRootIsAppShared(files: List<RepzTextFile>) {
    files
        .filter { !it.source.isBuildScript }
        .assertTrue("A4: @KoinApplication/startKoin/NavDisplay are used only in app:shared, the composition root (AGENTS.md §3.4)") { file ->
            val usesCompositionRootApi = A4_COMPOSITION_ROOT_TOKENS.any { it in file.text }
            !usesCompositionRootApi || file.source.modulePath == ":app:shared"
        }

    files
        .filter { !it.source.isBuildScript && it.source.modulePath.startsWith(":core:") }
        .assertTrue("A4: core:* never imports feature or app:shared code (AGENTS.md §3.4)") { file ->
            importedNames(file.text).none { it.startsWith("com.repz.app.feature.") || it.startsWith("com.repz.app.shared") }
        }
}

private val ENTITY_ANNOTATION = Regex("""@Entity\b""")
private val DTO_CLASS_NAME = Regex("""class\s+(\w*Dto)\b""")
private val RESTRICTED_ENTITY_OR_DTO_IMPORT = Regex("""com\.repz\.app\.core\.(?:database|network)\.\w*(?:Entity|Dto)\b""")
private val ALLOWED_ENTITY_DTO_MODULES = setOf(":core:database", ":core:network", ":core:data", ":core:sync")

/** A5: `@Entity` classes live only in `core:database`, `*Dto` classes only in `core:network`; neither leaks outside `core:database`/`core:network`/`core:data`/`core:sync` (AGENTS.md §3.5). */
fun checkA5EntitiesAndDtosStayInData(files: List<RepzTextFile>) {
    val mainFiles = files.filter { !it.source.isBuildScript }

    mainFiles.assertTrue("A5: @Entity classes live only in core:database (AGENTS.md §3.5)") { file ->
        !ENTITY_ANNOTATION.containsMatchIn(file.text) || file.source.modulePath == ":core:database"
    }

    mainFiles.assertTrue("A5: *Dto classes live only in core:network (AGENTS.md §3.5)") { file ->
        !DTO_CLASS_NAME.containsMatchIn(file.text) || file.source.modulePath == ":core:network"
    }

    mainFiles
        .filter { it.source.modulePath !in ALLOWED_ENTITY_DTO_MODULES }
        .assertTrue("A5: entity/DTO imports occur only in core:database/network/data/sync (AGENTS.md §3.5)") { file ->
            importedNames(file.text).none { RESTRICTED_ENTITY_OR_DTO_IMPORT.matches(it) }
        }
}
