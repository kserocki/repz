package com.repz.app.konsist.rules

import com.repz.app.konsist.support.RepzTextFile
import com.repz.app.konsist.support.assertTrue

// `runCatching`, `Result.mapCatching` and `Result.recoverCatching` are all inline stdlib functions
// that catch `Throwable` from their lambda, so all three swallow cancellation the same way. A
// trailing-lambda call (`runCatching { ... }`) has no parentheses at all, so only the bare
// identifier is guaranteed to appear; an extension-receiver call (`value.mapCatching { ... }` or
// `value.mapCatching(block)`) still contains the same identifier right after the dot. Lowercase
// only, so it never matches `suspendRunCatching`, whose own "RunCatching" substring starts with a
// capital R.
private val FORBIDDEN_CATCHING_CALLS = Regex("""\b(?:run|map|recover)Catching\b""")

// Every type that would also catch a thrown CancellationException: itself, plus its supertype
// chain up to Throwable. On every Kotlin target, CancellationException : IllegalStateException :
// RuntimeException : Exception : Throwable, so a catch of any of these silently swallows
// cancellation unless its body rethrows.
private val CANCELLATION_SWALLOWING_CATCH_TYPES =
    listOf("CancellationException", "IllegalStateException", "RuntimeException", "Exception", "Throwable")

// Captures the catch parameter's name, so the body check below can require that specific
// instance to be rethrown, not just any `throw` statement (a catch that throws a brand new,
// unrelated exception still swallows the original CancellationException).
private val CATCH_CLAUSE =
    Regex("""catch\s*\(\s*(\w+)\s*:\s*(?:\w+\.)*(?:${CANCELLATION_SWALLOWING_CATCH_TYPES.joinToString("|")})\s*\)\s*\{""")

private const val HELPER_FILE_PATH = "core/common/src/commonMain/kotlin/com/repz/app/core/common/coroutines/CancellationSafety.kt"

/**
 * E1: cancellation safety (AGENTS.md §4). A plain `runCatching`/`mapCatching`/`recoverCatching`
 * (bare or as a `.xCatching(` extension-receiver call) and a hand-written `catch` of `Throwable`,
 * `Exception`, or any type in `CancellationException`'s own supertype chain (`RuntimeException`,
 * `IllegalStateException`, `CancellationException` itself) both swallow coroutine cancellation
 * unless the catch body rethrows that same caught value, directly (`throw e`) or through
 * `e.rethrowIfCancellation()`. `core:common`'s own helper file (`CancellationSafety.kt`) is exempt:
 * `suspendRunCatching` is the one place allowed to implement this behaviour, by calling the stdlib
 * `runCatching` and then rethrowing cancellation itself. Test source sets are exempt too: a test
 * asserts on a thrown exception with its own `assertFailsWith`/`try`-`catch`, which is not
 * production error handling and must stay free to catch broadly. Matching is plain text over the
 * whole file, including comments and strings, and the catch body is found by a simple brace count
 * rather than a real parse - the same trade-off I1 makes: stricter than the code deserves, but
 * simpler and safer than parsing around it.
 */
fun checkE1CancellationSafety(files: List<RepzTextFile>) {
    val checkedFiles =
        files.filter { file ->
            !file.source.isBuildScript &&
                !file.source.isTest &&
                file.source.modulePath != ":konsist-tests" &&
                !file.path.endsWith(HELPER_FILE_PATH)
        }

    checkedFiles.assertTrue(
        "E1: no runCatching/mapCatching/recoverCatching (bare or as .xCatching(), they swallow " +
            "CancellationException - use com.repz.app.core.common.coroutines.suspendRunCatching instead (AGENTS.md §4)",
    ) { file -> !FORBIDDEN_CATCHING_CALLS.containsMatchIn(file.text) }

    checkedFiles.assertTrue(
        "E1: a catch of Throwable/Exception/RuntimeException/IllegalStateException/CancellationException " +
            "must rethrow that value (throw it, or call its rethrowIfCancellation()) (AGENTS.md §4)",
    ) { file -> hasNoUnsafeCatch(file.text) }
}

private fun hasNoUnsafeCatch(text: String): Boolean =
    CATCH_CLAUSE.findAll(text).all { match ->
        val paramName = match.groupValues[1]
        val openBraceIndex = match.range.last
        val closeBraceIndex = matchingBraceIndex(text, openBraceIndex)
        val body = text.substring(openBraceIndex + 1, closeBraceIndex)
        bodyRethrows(paramName, body)
    }

private fun bodyRethrows(
    paramName: String,
    body: String,
): Boolean {
    // The negative lookahead rejects `throw e.toDomainError()`, `throw e.cause ?: e` and similar:
    // a word boundary alone sits between `e` and `.`, so without it a catch that wraps the caught
    // value in a different exception - losing its cancellation-ness - would still count as a
    // rethrow. Only a bare `throw e` (as its own statement) is accepted.
    val throwsParam = Regex("""\bthrow\s+$paramName\b(?!\s*[.?(\[!:])""")
    val callsRethrowIfCancellation = Regex("""\b$paramName\.rethrowIfCancellation\s*\(""")
    return throwsParam.containsMatchIn(body) || callsRethrowIfCancellation.containsMatchIn(body)
}

/** The index of the `}` that closes the `{` at [openBraceIndex], tracking nested brace depth. */
private fun matchingBraceIndex(
    text: String,
    openBraceIndex: Int,
): Int {
    var depth = 0
    for (index in openBraceIndex until text.length) {
        when (text[index]) {
            '{' -> {
                depth++
            }

            '}' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return text.length
}
