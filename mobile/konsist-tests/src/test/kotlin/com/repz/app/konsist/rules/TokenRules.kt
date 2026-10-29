package com.repz.app.konsist.rules

import com.repz.app.konsist.support.RepzTextFile
import com.repz.app.konsist.support.assertTrue
import com.repz.app.konsist.support.hasImportStartingWith

private val UI_SCOPED_MODULES = listOf(Regex(""":feature:[^:]+:impl"""), Regex("""^:core:ui$"""), Regex("""^:app:shared$"""))
private val COLOR_CALL = Regex("""\bColor\(""")
private val COLOR_NAMED_CONSTANT = Regex("""\bColor\.(\w+)""")
private val ALLOWED_COLOR_CONSTANTS = setOf("Transparent", "Unspecified")
private val DP_LITERAL = Regex("""(\d+(?:\.\d+)?)\.dp\b""")
private val SP_LITERAL = Regex("""\d+(?:\.\d+)?\.sp\b""")
private val SPRING_OR_TWEEN_CALL = Regex("""\b(?:spring|tween)\(""")

/**
 * T1: UI files outside `core:designsystem` contain no hard-coded `Color(…)`/`Color.<Name>`, no
 * `<number>.dp`, no `<number>.sp`, no `spring(`/`tween(` call. Allowed: `0.dp`, `Color.Transparent`,
 * `Color.Unspecified` (AGENTS.md §3.7, MOTION.md; Q1). A "UI file" is a main file importing
 * `androidx.compose.` in `feature:x:impl`, `core:ui` or `app:shared`.
 */
fun checkT1NoHardcodedDesignTokens(files: List<RepzTextFile>) {
    files
        .filter(::isUiFile)
        .assertTrue("T1: no hard-coded colors/dp/sp/spring/tween outside core:designsystem (AGENTS.md §3.7)") { file ->
            val text = file.text
            !COLOR_CALL.containsMatchIn(text) &&
                COLOR_NAMED_CONSTANT.findAll(text).all { it.groupValues[1] in ALLOWED_COLOR_CONSTANTS } &&
                DP_LITERAL.findAll(text).all { it.groupValues[1].toDouble() == 0.0 } &&
                !SP_LITERAL.containsMatchIn(text) &&
                !SPRING_OR_TWEEN_CALL.containsMatchIn(text)
        }
}

private fun isUiFile(file: RepzTextFile): Boolean =
    !file.source.isBuildScript &&
        !file.source.isTest &&
        file.source.modulePath != ":core:designsystem" &&
        UI_SCOPED_MODULES.any { it.matches(file.source.modulePath) } &&
        hasImportStartingWith(file.text, "androidx.compose.")
