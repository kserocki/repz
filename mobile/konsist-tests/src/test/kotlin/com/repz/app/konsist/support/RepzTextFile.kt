package com.repz.app.konsist.support

import com.lemonappdev.konsist.api.Konsist
import java.io.File

/** A `.kt` or `.kts` file read as plain text, with [RepzSourceFile] metadata attached. */
data class RepzTextFile(
    val path: String,
    val text: String,
    val source: RepzSourceFile,
)

private val IGNORED_DIRECTORY_NAMES = setOf("build", ".gradle", ".kotlin", ".git", ".idea")
private val mobileRoot: File by lazy { File(Konsist.projectRootPath) }

/**
 * Every `.kt`/`.kts` file under the real `mobile/` tree, minus every fixture tree, matched by its
 * exact relative path from the project root. Konsist's `KotlinFileParser` rejects `.kts` outright,
 * so text-only rules (C1) read every file directly instead of through a [com.lemonappdev.konsist.api.container.KoScope].
 */
fun realTextFiles(): List<RepzTextFile> = repzTextFilesUnder(mobileRoot) { dir -> !isUnderFixturesRoot(relativeToMobileRoot(dir)) }

/** The fixture tree for one rule, read the same way as [realTextFiles]. */
fun fixtureTextFiles(ruleId: String): List<RepzTextFile> =
    repzTextFilesUnder(File(mobileRoot, "${FIXTURES_ROOT_SEGMENTS.joinToString("/")}/$ruleId"))

private fun relativeToMobileRoot(file: File): String = file.path.removePrefix(mobileRoot.path)

private fun repzTextFilesUnder(
    root: File,
    enterDirectory: (File) -> Boolean = { true },
): List<RepzTextFile> =
    root
        .walkTopDown()
        .onEnter { dir -> dir.name !in IGNORED_DIRECTORY_NAMES && enterDirectory(dir) }
        .filter { it.isFile && (it.extension == "kt" || it.extension == "kts") }
        .map { file ->
            RepzTextFile(
                path = file.path,
                text = file.readText(),
                source = parseRepzSourceFile(relativeToMobileRoot(file)),
            )
        }.toList()
