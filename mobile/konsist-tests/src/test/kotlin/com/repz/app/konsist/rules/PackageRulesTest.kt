package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureScope
import com.repz.app.konsist.support.projectScope
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

class PackageRulesTest {
    @Test
    fun given_project_whenP1Checked_thenNoViolations() {
        checkP1PackageMatchesModule(projectScope())
    }

    @Test
    fun given_p1ViolationFixture_whenChecked_thenFailsWithP1() {
        val error = assertFails { checkP1PackageMatchesModule(fixtureScope("p1")) }

        assertContains(error.message.orEmpty(), "P1")
    }
}
