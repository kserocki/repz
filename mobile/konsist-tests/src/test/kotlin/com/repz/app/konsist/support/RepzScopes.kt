package com.repz.app.konsist.support

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope

private val FIXTURES_ROOT = FIXTURES_ROOT_SEGMENTS.joinToString("/")

/**
 * The real `mobile/` tree, minus every fixture tree, matched by its exact relative path from the
 * project root (ARCH §12.2). A4/A5 additionally exclude `:konsist-tests` themselves, since they
 * could otherwise match their own source text.
 */
fun projectScope(): KoScope =
    Konsist
        .scopeFromProject(ignoreBuildConfig = false)
        .slice { file -> !isUnderFixturesRoot(file.projectPath) }

/** The fixture tree for one rule, e.g. `fixtureScope("a3")` for everything under `fixtures/a3`. */
fun fixtureScope(ruleId: String): KoScope = Konsist.scopeFromDirectory("$FIXTURES_ROOT/$ruleId")

internal fun isUnderFixturesRoot(path: String): Boolean = pathSegments(path).take(FIXTURES_ROOT_SEGMENTS.size) == FIXTURES_ROOT_SEGMENTS
