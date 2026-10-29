package com.repz.app.konsist.rules

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertTrue
import com.repz.app.konsist.support.repzSourceFile

/**
 * P1: main files under `core`, `feature` (api and impl) and `app/shared` declare a package that
 * starts with the module's namespace (`com.repz.app.` plus its path, AGENTS.md §3, ADR-0004);
 * `app/android` main files only need to start with `com.repz.app` (its real namespace is the fixed
 * `com.repz.app`, set directly in its build file, not the one this algorithm would derive for
 * `:app:android`).
 */
fun checkP1PackageMatchesModule(scope: KoScope) {
    scope.files
        .filter { file ->
            val source = file.repzSourceFile()
            !source.isTest && !source.isBuildScript && isP1Scoped(source.modulePath)
        }.assertTrue(
            additionalMessage = "P1: package must start with the module namespace (AGENTS.md §3)",
        ) { file ->
            val modulePath = file.repzSourceFile().modulePath
            val packageName = file.packagee?.name.orEmpty()
            if (modulePath == ":app:android") {
                packageName.startsWith("com.repz.app")
            } else {
                packageName.startsWith(expectedNamespace(modulePath))
            }
        }
}

private fun isP1Scoped(modulePath: String): Boolean =
    modulePath.startsWith(":core:") || modulePath.startsWith(":feature:") || modulePath == ":app:shared" || modulePath == ":app:android"

/** Mirrors `Project.repzNamespace()` in build-logic: drops the leading `app` segment for `:app:*`. */
internal fun expectedNamespace(modulePath: String): String {
    val segments = modulePath.removePrefix(":").split(":").filter { it.isNotEmpty() }
    val withoutAppPrefix = if (segments.firstOrNull() == "app") segments.drop(1) else segments
    return (listOf("com", "repz", "app") + withoutAppPrefix).joinToString(".")
}
