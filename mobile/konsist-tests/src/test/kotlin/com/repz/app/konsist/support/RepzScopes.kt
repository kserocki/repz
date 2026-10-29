package com.repz.app.konsist.support

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope

private const val FIXTURES_ROOT = "konsist-tests/src/test/fixtures"

/**
 * The real `mobile/` tree, minus every fixture under [FIXTURES_ROOT] (§3.5) and minus
 * `:konsist-tests` itself: it hosts the rules, so its own source text contains the very patterns
 * (annotation names, token lists, regexes) the heuristic rules search for, which would otherwise
 * make a rule fail against its own implementation. It is tooling, not one of the modules AGENTS.md's
 * architecture rules describe.
 */
fun projectScope(): KoScope =
    Konsist
        .scopeFromProject(ignoreBuildConfig = false)
        .slice { file -> FIXTURES_ROOT !in file.path && "/konsist-tests/" !in file.path }

/** The fixture tree for one rule, e.g. `fixtureScope("a3")` for everything under `fixtures/a3`. */
fun fixtureScope(ruleId: String): KoScope = Konsist.scopeFromDirectory("$FIXTURES_ROOT/$ruleId")
