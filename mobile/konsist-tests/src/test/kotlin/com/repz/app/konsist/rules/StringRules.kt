package com.repz.app.konsist.rules

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertTrue
import com.repz.app.konsist.support.repzSourceFile

private val S1_UI_MODULES =
    listOf(
        Regex(""":feature:[^:]+:impl"""),
        Regex("""^:core:ui$"""),
        Regex("""^:core:designsystem$"""),
        Regex("""^:app:shared$"""),
    )
private val FORBIDDEN_STRING_CALL_ARGS = listOf("text", "contentDescription", "label", "placeholder", "title")
private val TEXT_CALL_STRING_ARG = Regex("""\bText\(\s*"([^"]*)"""")
private val NAMED_ARG_STRING = Regex("""\b(?:${FORBIDDEN_STRING_CALL_ARGS.joinToString("|")})\s*=\s*"([^"]*)"""")
private val HAS_LETTER = Regex("""\p{L}""")

/**
 * S1: UI files in `feature:x:impl`, `core:ui`, `core:designsystem` and `app:shared` pass no string
 * literal containing a letter to `Text(`, or to a `text =`/`contentDescription =`/`label =`/
 * `placeholder =`/`title =` argument, outside `@Preview` functions (AGENTS.md §4 Strings; all
 * user-facing text goes through Compose Resources, EN + PL). Heuristic (text-based), per function.
 */
fun checkS1NoHardcodedUserFacingStrings(scope: KoScope) {
    scope
        .functions()
        .filter { function ->
            val source = function.repzSourceFile()
            !source.isTest && S1_UI_MODULES.any { it.matches(source.modulePath) } && !function.hasAnnotationWithName("Preview")
        }.assertTrue(
            additionalMessage = "S1: no hard-coded user-facing strings, use Compose Resources (AGENTS.md §4 Strings)",
        ) { function ->
            !hasForbiddenStringLiteral(function.text)
        }
}

private fun hasForbiddenStringLiteral(text: String): Boolean =
    (TEXT_CALL_STRING_ARG.findAll(text) + NAMED_ARG_STRING.findAll(text)).any { HAS_LETTER.containsMatchIn(it.groupValues[1]) }
