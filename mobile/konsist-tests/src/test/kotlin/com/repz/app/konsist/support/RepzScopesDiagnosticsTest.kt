package com.repz.app.konsist.support

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Prints what [projectScope] and [realTextFiles] actually resolved, so Konsist finding the wrong
 * project root (or silently returning zero files) shows up as readable output, not as every rule
 * quietly passing vacuously. Also asserts the facts every rule in this module depends on.
 */
class RepzScopesDiagnosticsTest {
    @Test
    fun given_projectScope_whenInspected_thenEveryFileResolvesUnderMobileAndCountsArePrinted() {
        val files = projectScope().files

        println("Konsist project scope: ${files.size} files")
        val byModule = files.groupBy { it.repzSourceFile().modulePath }
        byModule.toSortedMap().forEach { (modulePath, filesInModule) ->
            val bySourceSet = filesInModule.groupBy { it.repzSourceFile().sourceSetName ?: "(script)" }
            println("  $modulePath: " + bySourceSet.entries.joinToString { (sourceSet, f) -> "$sourceSet=${f.size}" })
        }

        assertTrue(files.isNotEmpty(), "projectScope() found no files at all - Konsist likely resolved the wrong root.")
        assertTrue(
            files.all {
                it.path
                    .replace('\\', '/')
                    .substringAfterLast("/mobile/", missingDelimiterValue = "")
                    .isNotEmpty()
            },
            "Every file must resolve under a `mobile/` directory; projectScope() likely found the wrong root.",
        )
    }

    @Test
    fun given_realTextFiles_whenInspected_thenCoversBuildScriptsAndBuildLogic() {
        val files = realTextFiles()

        println("Real text files: ${files.size} files")

        assertTrue(files.isNotEmpty(), "realTextFiles() found no files at all - the fixtures exclusion likely matched everything.")
        assertTrue(files.any { it.source.isBuildScript }, "realTextFiles() must include at least one .kts build script.")
        assertTrue(files.any { it.source.modulePath.startsWith(":build-logic:") }, "realTextFiles() must include build-logic sources.")
    }
}
