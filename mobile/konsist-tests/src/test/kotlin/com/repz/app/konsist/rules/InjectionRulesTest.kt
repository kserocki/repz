package com.repz.app.konsist.rules

import com.repz.app.konsist.support.fixtureTextFiles
import com.repz.app.konsist.support.realTextFiles
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

private const val TIME_READ_MESSAGE = "I1: wall-clock time is read only in the composition root's CommonModule"
private const val ID_GENERATION_MESSAGE = "I1: ids are generated only in the id generator"

class InjectionRulesTest {
    @Test
    fun given_project_whenI1Checked_thenNoViolations() {
        checkI1TimeAndIdsAreInjected(realTextFiles())
    }

    @Test
    fun given_i1ClockSystemFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-clock-system")) }

        assertContains(error.message.orEmpty(), TIME_READ_MESSAGE)
    }

    @Test
    fun given_i1UuidRandomFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-uuid-random")) }

        assertContains(error.message.orEmpty(), ID_GENERATION_MESSAGE)
    }

    @Test
    fun given_i1UuidGenerateV4Fixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-uuid-generate-v4")) }

        assertContains(error.message.orEmpty(), ID_GENERATION_MESSAGE)
    }

    @Test
    fun given_i1UuidGenerateV7Fixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-uuid-generate-v7")) }

        assertContains(error.message.orEmpty(), ID_GENERATION_MESSAGE)
    }

    @Test
    fun given_i1ClockSystemTestSourceFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-clock-system-test-source")) }

        assertContains(error.message.orEmpty(), TIME_READ_MESSAGE)
    }

    @Test
    fun given_i1SameNamedFileAtTheWrongPathFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-same-named-file-wrong-path")) }

        assertContains(error.message.orEmpty(), TIME_READ_MESSAGE)
    }

    @Test
    fun given_i1ClockSystemMemberImportFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-clock-system-member-import")) }

        assertContains(error.message.orEmpty(), TIME_READ_MESSAGE)
    }

    @Test
    fun given_i1ClockSystemAliasedImportFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-clock-system-aliased-import")) }

        assertContains(error.message.orEmpty(), TIME_READ_MESSAGE)
    }

    @Test
    fun given_i1SystemCurrentTimeMillisFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-system-current-time-millis")) }

        assertContains(error.message.orEmpty(), TIME_READ_MESSAGE)
    }

    @Test
    fun given_i1NsDateFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-nsdate")) }

        assertContains(error.message.orEmpty(), TIME_READ_MESSAGE)
    }

    @Test
    fun given_i1UuidRandomUuidFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-uuid-randomuuid")) }

        assertContains(error.message.orEmpty(), ID_GENERATION_MESSAGE)
    }

    @Test
    fun given_i1NsUuidFixture_whenChecked_thenFailsWithI1() {
        val error = assertFails { checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-nsuuid")) }

        assertContains(error.message.orEmpty(), ID_GENERATION_MESSAGE)
    }

    @Test
    fun given_i1AllowedClockSystemFixture_whenChecked_thenNoViolations() {
        checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-allowed-clock-system"))
    }

    @Test
    fun given_i1AllowedUuidGenerationFixture_whenChecked_thenNoViolations() {
        checkI1TimeAndIdsAreInjected(fixtureTextFiles("i1-allowed-uuid-generation"))
    }
}
