package com.repz.app.konsist.rules

import com.repz.app.konsist.support.RepzTextFile
import com.repz.app.konsist.support.assertTrue

private val VALID_TODO = Regex("""TODO\(REP-\d+\):""")
private val REP_KEY = Regex("""REP-\d+""")
private val TODO_WORD = Regex("""\bTODO\b""")

/**
 * C1: in every comment, a `REP-<n>` key appears only as `TODO(REP-<n>):`, and every `TODO` is
 * written exactly that way; upstream references (`KT-…`, `lib#123`) are unrestricted (AGENTS.md §4,
 * §8; Q2). Scanned as plain text with this file's own comment extractor, so it also covers `.kts`
 * build scripts, which Konsist 0.18.1 cannot parse at all.
 */
fun checkC1OnlyLinearKeysInTodoForm(files: List<RepzTextFile>) {
    files.assertTrue("C1: a REP-<n> key in a comment only ever appears as TODO(REP-<n>): (AGENTS.md §4, §8)") { file ->
        extractComments(file.text).none { comment -> commentViolatesC1(comment) }
    }
}

private fun commentViolatesC1(comment: String): Boolean {
    val validSpans = VALID_TODO.findAll(comment).map { it.range }.toList()

    fun startsAValidSpan(position: Int) = validSpans.any { it.first == position }

    fun isCoveredByAValidSpan(range: IntRange) = validSpans.any { range.first >= it.first && range.last <= it.last }

    val everyTodoIsValid = TODO_WORD.findAll(comment).all { startsAValidSpan(it.range.first) }
    val everyRepKeyIsInsideAValidTodo = REP_KEY.findAll(comment).all { isCoveredByAValidSpan(it.range) }
    return !everyTodoIsValid || !everyRepKeyIsInsideAValidTodo
}

/**
 * Every `//` line comment and `/* */` block comment in [text], as the text between the markers.
 * String and character literals are skipped (so a comment marker inside one is not mistaken for a
 * real comment start); triple-quoted strings are skipped whole. Kotlin's nestable block comments
 * are tracked by depth.
 */
internal fun extractComments(text: String): List<String> {
    val comments = mutableListOf<String>()
    var index = 0

    while (index < text.length) {
        index =
            when {
                text.startsWith("\"\"\"", index) -> skipTripleQuotedString(text, index)
                text[index] == '"' -> skipLiteral(text, index, '"')
                text[index] == '\'' -> skipLiteral(text, index, '\'')
                text.startsWith("//", index) -> readLineComment(text, index, comments)
                text.startsWith("/*", index) -> readBlockComment(text, index, comments)
                else -> index + 1
            }
    }

    return comments
}

private fun skipTripleQuotedString(
    text: String,
    start: Int,
): Int {
    val end = text.indexOf("\"\"\"", start + 3)
    return if (end == -1) text.length else end + 3
}

private fun skipLiteral(
    text: String,
    start: Int,
    quote: Char,
): Int {
    var index = start + 1
    while (index < text.length && text[index] != quote) {
        index += if (text[index] == '\\') 2 else 1
    }
    return (index + 1).coerceAtMost(text.length)
}

private fun readLineComment(
    text: String,
    start: Int,
    comments: MutableList<String>,
): Int {
    val end = text.indexOf('\n', start).let { if (it == -1) text.length else it }
    comments += text.substring(start + 2, end)
    return end
}

/** Kotlin's block comments nest, so depth is tracked until this one's own closing marker is found. */
private fun readBlockComment(
    text: String,
    start: Int,
    comments: MutableList<String>,
): Int {
    var index = start + 2
    var depth = 1
    while (index < text.length && depth > 0) {
        when {
            text.startsWith("/*", index) -> {
                depth++
                index += 2
            }

            text.startsWith("*/", index) -> {
                depth--
                index += 2
            }

            else -> {
                index++
            }
        }
    }
    val contentEnd = (if (depth == 0) index - 2 else index).coerceAtLeast(start + 2)
    comments += text.substring(start + 2, contentEnd)
    return index
}
