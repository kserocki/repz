package com.repz.app.konsist.rules

import com.repz.app.konsist.support.RepzTextFile
import com.repz.app.konsist.support.assertTrue

private val CLOCK_SYSTEM = Regex("""\bClock\.System\b""")
private val UUID_GENERATION = Regex("""\bUuid\.(?:random|generateV4|generateV7)\s*\(""")

private const val ALLOWED_CLOCK_SYSTEM_PATH = "app/shared/src/commonMain/kotlin/com/repz/app/shared/di/CommonModule.kt"
private const val ALLOWED_UUID_GENERATION_PATH = "core/common/src/commonMain/kotlin/com/repz/app/core/common/id/UuidV7IdGenerator.kt"

/**
 * I1: time and ids are injected (AGENTS.md §4). `Clock.System` is read only in the composition
 * root's `CommonModule`; `Uuid.random`/`generateV4`/`generateV7` are called only in the id
 * generator. Every source set is covered, tests included, because deterministic tests must inject
 * a fixed `Clock`/`IdGenerator` too. `konsist-tests` itself and its fixtures are excluded, the same
 * way A4/A5 exclude them (this rule's own source otherwise matches its own patterns).
 */
fun checkI1TimeAndIdsAreInjected(files: List<RepzTextFile>) {
    val projectFiles = files.filter { !it.source.isBuildScript && it.source.modulePath != ":konsist-tests" }

    projectFiles.assertTrue("I1: Clock.System is read only in the composition root's CommonModule (AGENTS.md §4)") { file ->
        !CLOCK_SYSTEM.containsMatchIn(file.text) || file.path.endsWith(ALLOWED_CLOCK_SYSTEM_PATH)
    }

    projectFiles.assertTrue("I1: Uuid.random/generateV4/generateV7 are called only in the id generator (AGENTS.md §4)") { file ->
        !UUID_GENERATION.containsMatchIn(file.text) || file.path.endsWith(ALLOWED_UUID_GENERATION_PATH)
    }
}
