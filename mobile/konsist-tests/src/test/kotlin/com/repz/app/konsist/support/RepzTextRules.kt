package com.repz.app.konsist.support

private val IMPORT_LINE = Regex("""^\s*import\s+([\w.]+)""", RegexOption.MULTILINE)

/** Every top-level `import` statement's fully qualified name in [text]. */
fun importedNames(text: String): List<String> = IMPORT_LINE.findAll(text).map { it.groupValues[1] }.toList()

/** Whether [text] imports a name starting with any of [prefixes]. */
fun hasImportStartingWith(
    text: String,
    vararg prefixes: String,
): Boolean = importedNames(text).any { name -> prefixes.any { prefix -> name.startsWith(prefix) } }

/**
 * Mirrors Konsist's `assertTrue` (see [com.lemonappdev.konsist.api.verify.assertTrue]) for plain
 * [RepzTextFile] lists, used where a rule needs `.kts` build files too (Konsist 0.18.1's
 * `KotlinFileParser` rejects `.kts` outright). Lists every failing file's path on failure.
 */
fun List<RepzTextFile>.assertTrue(
    additionalMessage: String,
    predicate: (RepzTextFile) -> Boolean,
) {
    val violations = filterNot(predicate)
    check(violations.isEmpty()) {
        buildString {
            appendLine(additionalMessage)
            violations.forEach { appendLine(" - ${it.path}") }
        }
    }
}
