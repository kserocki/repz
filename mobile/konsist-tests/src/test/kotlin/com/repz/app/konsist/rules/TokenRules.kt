package com.repz.app.konsist.rules

import com.repz.app.konsist.support.RepzTextFile
import com.repz.app.konsist.support.assertTrue
import com.repz.app.konsist.support.hasImportStartingWith

private val UI_SCOPED_MODULES = listOf(Regex(""":feature:[^:]+:impl"""), Regex("""^:core:ui$"""), Regex("""^:app:shared$"""))
private val COLOR_CALL = Regex("""\bColor\(""")
private val COLOR_NAMED_CONSTANT = Regex("""\bColor\.(\w+)""")
private val ALLOWED_COLOR_CONSTANTS = setOf("Transparent", "Unspecified")
private val DP_LITERAL = Regex("""(\d+(?:\.\d+)?)f?\.dp\b""")
private val SP_LITERAL = Regex("""\d+(?:\.\d+)?f?\.sp\b""")

// Optional generic type arguments, e.g. `spring<Float>(...)`, `tween<Dp>(300)`, are still a call.
private val SPRING_OR_TWEEN_CALL = Regex("""\b(?:spring|tween)\s*(?:<[^>]*>)?\(""")

/**
 * T1: UI files outside `core:designsystem` contain no hard-coded `Color(…)`/`Color.<Name>`, no
 * `<number>.dp`, no `<number>.sp`, no `spring(`/`tween(` call. Allowed: `0.dp`, `Color.Transparent`,
 * `Color.Unspecified` (AGENTS.md §3.7, MOTION.md). A "UI file" is a main file importing
 * `androidx.compose.` in `feature:x:impl`, `core:ui` or `app:shared`.
 */
fun checkT1NoHardcodedDesignTokens(files: List<RepzTextFile>) {
    val uiFiles = files.filter(::isUiFile)

    uiFiles.assertTrue("T1: no hard-coded Color(…) calls (AGENTS.md §3.7)") { file -> !COLOR_CALL.containsMatchIn(file.text) }

    uiFiles.assertTrue("T1: only Color.Transparent/Color.Unspecified are allowed named colors (AGENTS.md §3.7)") { file ->
        COLOR_NAMED_CONSTANT.findAll(file.text).all { it.groupValues[1] in ALLOWED_COLOR_CONSTANTS }
    }

    uiFiles.assertTrue("T1: only 0.dp is an allowed hard-coded dp literal (AGENTS.md §3.7)") { file ->
        DP_LITERAL.findAll(file.text).all { it.groupValues[1].toDouble() == 0.0 }
    }

    uiFiles.assertTrue("T1: no hard-coded sp literals (AGENTS.md §3.7)") { file -> !SP_LITERAL.containsMatchIn(file.text) }

    uiFiles.assertTrue("T1: no hard-coded spring(/tween( calls (AGENTS.md §3.7)") { file ->
        !SPRING_OR_TWEEN_CALL.containsMatchIn(file.text)
    }
}

private fun isUiFile(file: RepzTextFile): Boolean =
    !file.source.isBuildScript &&
        !file.source.isTest &&
        UI_SCOPED_MODULES.any { it.matches(file.source.modulePath) } &&
        hasImportStartingWith(file.text, "androidx.compose.")
