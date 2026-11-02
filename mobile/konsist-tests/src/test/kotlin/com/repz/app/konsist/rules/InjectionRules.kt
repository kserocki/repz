package com.repz.app.konsist.rules

import com.repz.app.konsist.support.RepzTextFile
import com.repz.app.konsist.support.assertTrue

// `Clock.System` also matches a member/aliased import of it (`import kotlin.time.Clock.System`
// is still the literal substring "Clock.System"), so a later bare `System.now()` is still caught
// at the import line. `System.currentTimeMillis()`/`NSDate()` are the JVM/Darwin ways around
// `Clock` entirely.
private val TIME_READ = Regex("""\bClock\.System\b|\bSystem\.currentTimeMillis\s*\(|\bNSDate\(\)""")

// Likewise `UUID.randomUUID()`/`NSUUID()` are the JVM/Darwin ways to mint an id without going
// through `Uuid`.
private val ID_GENERATION = Regex("""\bUuid\.(?:random|generateV4|generateV7)\s*\(|\bUUID\.randomUUID\s*\(|\bNSUUID\(\)""")

private const val ALLOWED_CLOCK_SYSTEM_PATH = "app/shared/src/commonMain/kotlin/com/repz/app/shared/di/CommonModule.kt"
private const val ALLOWED_UUID_GENERATION_PATH = "core/common/src/commonMain/kotlin/com/repz/app/core/common/id/UuidV7IdGenerator.kt"

/**
 * I1: time and ids are injected (AGENTS.md §4). Wall-clock time is read only in the composition
 * root's `CommonModule`; ids are minted only in the id generator. Every source set is covered,
 * tests included, because deterministic tests must inject a fixed `Clock`/`IdGenerator` too.
 * `konsist-tests` itself and its fixtures are excluded, the same way A4/A5 exclude them (this
 * rule's own source otherwise matches its own patterns). Matching is plain text, so it also
 * catches a match inside a comment or a string literal - stricter than the code deserves, but
 * simpler and safer than trying to parse around it.
 */
fun checkI1TimeAndIdsAreInjected(files: List<RepzTextFile>) {
    val projectFiles = files.filter { !it.source.isBuildScript && it.source.modulePath != ":konsist-tests" }

    projectFiles.assertTrue(
        "I1: wall-clock time is read only in the composition root's CommonModule " +
            "(Clock.System; also forbidden: System.currentTimeMillis(), NSDate()) (AGENTS.md §4)",
    ) { file ->
        !TIME_READ.containsMatchIn(file.text) || file.path.endsWith(ALLOWED_CLOCK_SYSTEM_PATH)
    }

    projectFiles.assertTrue(
        "I1: ids are generated only in the id generator " +
            "(Uuid.random/generateV4/generateV7; also forbidden: UUID.randomUUID(), NSUUID()) (AGENTS.md §4)",
    ) { file ->
        !ID_GENERATION.containsMatchIn(file.text) || file.path.endsWith(ALLOWED_UUID_GENERATION_PATH)
    }
}
