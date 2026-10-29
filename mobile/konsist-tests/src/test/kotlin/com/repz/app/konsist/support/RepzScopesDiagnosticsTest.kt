package com.repz.app.konsist.support

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Prints what [projectScope] actually resolved (R5: Konsist might find the wrong project root, or
 * silently return zero files) so a wrong root shows up as readable output, not as 24 rules quietly
 * passing vacuously. Also asserts the one fact every other rule in this module depends on: every
 * file's path starts under `mobile/`.
 */
class RepzScopesDiagnosticsTest {
    @Test
    fun givenProjectScope_whenInspected_thenEveryFileResolvesUnderMobileAndCountsArePrinted() {
        val files = projectScope().files

        println("Konsist project scope: ${files.size} files")
        val byModule = files.groupBy { it.repzSourceFile().modulePath }
        byModule.toSortedMap().forEach { (modulePath, filesInModule) ->
            val bySourceSet = filesInModule.groupBy { it.repzSourceFile().sourceSetName ?: "(script)" }
            println("  $modulePath: " + bySourceSet.entries.joinToString { (sourceSet, f) -> "$sourceSet=${f.size}" })
        }

        assertTrue(files.isNotEmpty(), "projectScope() found no files at all - Konsist likely resolved the wrong root (R5).")
        assertTrue(
            files.all {
                it.path
                    .replace('\\', '/')
                    .substringAfterLast("/mobile/", missingDelimiterValue = "")
                    .isNotEmpty()
            },
            "Every file must resolve under a `mobile/` directory; projectScope() likely found the wrong root (R5).",
        )
    }
}
