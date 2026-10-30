package com.repz.app.konsist.support

import com.lemonappdev.konsist.api.provider.KoPathProvider

/**
 * A file's Gradle module path (e.g. `:feature:history:impl`), source-set name (`commonMain`,
 * `iosTest`, …; `null` for a `.kts` build script), and whether it is test code or a build script,
 * derived from the file's own path relative to the project root (ARCH §12.2).
 */
data class RepzSourceFile(
    val modulePath: String,
    val sourceSetName: String?,
    val isTest: Boolean,
    val isBuildScript: Boolean,
)

/**
 * Parses [KoPathProvider.projectPath] into a [RepzSourceFile]. Works on a file or on any
 * declaration (class, function, property, …), which all carry the path of their containing file.
 */
fun KoPathProvider.repzSourceFile(): RepzSourceFile = parseRepzSourceFile(projectPath)

/** [path] split into forward-slash segments; a leading/trailing separator yields no empty segment. */
internal fun pathSegments(path: String): List<String> = path.replace('\\', '/').split('/').filter { it.isNotEmpty() }

/** A fixture file's segments start with exactly these before its fake module tree and ruleId. */
internal val FIXTURES_ROOT_SEGMENTS = listOf("konsist-tests", "src", "test", "fixtures")

internal fun parseRepzSourceFile(projectPath: String): RepzSourceFile {
    val segments = pathSegments(projectPath).withoutFixturesPrefix()
    val sourceDirectoryIndex =
        segments.indices.lastOrNull { index ->
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

/** Drops a leading `konsist-tests/src/test/fixtures/<ruleId>/`, matched by exact segments, not a substring search. */
private fun List<String>.withoutFixturesPrefix(): List<String> {
    val prefixLength = FIXTURES_ROOT_SEGMENTS.size + 1
    return if (size > prefixLength && subList(0, FIXTURES_ROOT_SEGMENTS.size) == FIXTURES_ROOT_SEGMENTS) drop(prefixLength) else this
}

private fun List<String>.toGradleModulePath(): String = ":" + joinToString(":")
