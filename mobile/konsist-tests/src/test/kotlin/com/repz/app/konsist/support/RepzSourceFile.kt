package com.repz.app.konsist.support

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration

/**
 * A file's Gradle module path (e.g. `:feature:history:impl`), source-set name (`commonMain`,
 * `iosTest`, …; `null` for a `.kts` build script), and whether it is test code or a build script —
 * derived purely from the file's own path. This runs unchanged for the real project tree and for
 * fixture trees laid out as `konsist-tests/src/test/fixtures/<ruleId>/<module path>/src/<sourceSet>/kotlin/…`
 * (D9/ARCH §12): both resolve the module path from the segments before the last `src/<name>/kotlin`
 * directory.
 */
data class RepzSourceFile(
    val modulePath: String,
    val sourceSetName: String?,
    val isTest: Boolean,
    val isBuildScript: Boolean,
)

/** Parses [KoFileDeclaration.projectPath] into a [RepzSourceFile]. See [RepzSourceFile]. */
fun KoFileDeclaration.repzSourceFile(): RepzSourceFile = parseRepzSourceFile(projectPath)

/** A fixture file's path still has this real prefix before its fake module tree starts. */
private val FIXTURE_PREFIX = Regex(""".*konsist-tests/src/test/fixtures/[^/]+/(.*)""")

internal fun parseRepzSourceFile(projectPath: String): RepzSourceFile {
    val normalizedPath = projectPath.replace('\\', '/')
    val pathUnderModuleTree = FIXTURE_PREFIX.find(normalizedPath)?.groupValues?.get(1) ?: normalizedPath
    val segments = pathUnderModuleTree.split('/').filter { it.isNotEmpty() }
    val sourceDirectoryIndex = segments.indices.lastOrNull { index ->
        segments.getOrNull(index) == "src" && segments.getOrNull(index + 2) == "kotlin"
    }

    return if (sourceDirectoryIndex == null) {
        val fileName = segments.lastOrNull().orEmpty()
        RepzSourceFile(
            modulePath = segments.dropLast(1).toGradleModulePath(),
            sourceSetName = null,
            isTest = false,
            isBuildScript = fileName.endsWith(".kts"),
        )
    } else {
        val sourceSetName = segments[sourceDirectoryIndex + 1]
        RepzSourceFile(
            modulePath = segments.subList(0, sourceDirectoryIndex).toGradleModulePath(),
            sourceSetName = sourceSetName,
            isTest = sourceSetName.contains("test", ignoreCase = true),
            isBuildScript = false,
        )
    }
}

private fun List<String>.toGradleModulePath(): String = ":" + joinToString(":")
