package com.repz.buildlogic.contract

import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GenerateContractFixturesTaskTest {
    private val tempDirectory = createTempDirectory("contract-fixtures-test")

    @AfterTest
    fun tearDown() {
        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        tempDirectory.deleteRecursively()
    }

    @Test
    fun givenABackslashADollarAQuoteAndUnicode_whenEscapeKotlinStringLiteralIsCalled_thenEachIsEscaped() {
        val raw = "a\\b\"c\$d\ne\tf\u0007g"

        val escaped = escapeKotlinStringLiteral(raw)

        assertEquals("a\\\\b\\\"c\\\$d\\ne\\tf\\u0007g", escaped)
    }

    @Test
    fun givenARawStringWithEverySpecialCharacter_whenEscapedThenUnescaped_thenItRoundTrips() {
        val raw = "line one\nline \"two\" costs \$5\\path\u0001\ttab\rreturn"

        val roundTripped = unescapeKotlinStringLiteralForTest(escapeKotlinStringLiteral(raw))

        assertEquals(raw, roundTripped)
    }

    /** The inverse of [escapeKotlinStringLiteral], hand-rolled only to prove the round trip in this test. */
    private fun unescapeKotlinStringLiteralForTest(escaped: String): String {
        val result = StringBuilder()
        var index = 0
        while (index < escaped.length) {
            val character = escaped[index]
            if (character != '\\') {
                result.append(character)
                index++
                continue
            }
            when (val next = escaped[index + 1]) {
                '\\' -> result.append('\\')
                '"' -> result.append('"')
                '$' -> result.append('$')
                'n' -> result.append('\n')
                'r' -> result.append('\r')
                't' -> result.append('\t')
                'u' -> {
                    val hex = escaped.substring(index + 2, index + 6)
                    result.append(hex.toInt(16).toChar())
                    index += 4
                }
                else -> error("Unexpected escape: \\$next")
            }
            index += 2
        }
        return result.toString()
    }

    @Test
    fun givenFixturesInMixedOrder_whenCollectContractFixturesIsCalled_thenTheyAreSortedByPath() {
        writeFixture("syncPull/200/last-page.json", "{}")
        writeFixture("login/request/valid.json", "{}")
        writeFixture("components/ProblemDetails/not-found.json", "{}")

        val fixtures = collectContractFixtures(tempDirectory.toFile())

        assertEquals(
            listOf("components/ProblemDetails/not-found.json", "login/request/valid.json", "syncPull/200/last-page.json"),
            fixtures.map { it.path },
        )
    }

    @Test
    fun givenAnOperationRequestFixture_whenCollectContractFixturesIsCalled_thenItIsAccepted() {
        writeFixture("login/request/valid.json", "{\"email\":\"a@b.com\"}")

        val fixtures = collectContractFixtures(tempDirectory.toFile())

        assertEquals(1, fixtures.size)
    }

    @Test
    fun givenAnOperationStatusFixture_whenCollectContractFixturesIsCalled_thenItIsAccepted() {
        writeFixture("login/200/session.json", "{}")

        val fixtures = collectContractFixtures(tempDirectory.toFile())

        assertEquals(1, fixtures.size)
    }

    @Test
    fun givenAComponentFixture_whenCollectContractFixturesIsCalled_thenItIsAccepted() {
        writeFixture("components/ProblemDetails/not-found.json", "{}")

        val fixtures = collectContractFixtures(tempDirectory.toFile())

        assertEquals(1, fixtures.size)
    }

    @Test
    fun givenANonJsonFile_whenCollectContractFixturesIsCalled_thenItFailsNamingTheFile() {
        writeFixture("login/request/valid.txt", "not json")

        val exception = assertFailsWith<IllegalStateException> { collectContractFixtures(tempDirectory.toFile()) }

        assertTrue(exception.message.orEmpty().contains("login/request/valid.txt"))
    }

    @Test
    fun givenAnUnknownFolderLayout_whenCollectContractFixturesIsCalled_thenItFailsNamingTheFolder() {
        writeFixture("login/unexpectedFolder/valid.json", "{}")

        val exception = assertFailsWith<IllegalStateException> { collectContractFixtures(tempDirectory.toFile()) }

        assertTrue(exception.message.orEmpty().contains("login/unexpectedFolder/valid.json"))
    }

    @Test
    fun givenAFixtureOverTheSizeLimit_whenCollectContractFixturesIsCalled_thenItFails() {
        writeFixture("login/request/huge.json", "x".repeat(60_001))

        assertFailsWith<IllegalStateException> { collectContractFixtures(tempDirectory.toFile()) }
    }

    @Test
    fun givenTwoFixtures_whenGeneratedContractFixturesSourceIsCalled_thenBothAppearInDeterministicOrder() {
        val fixtures =
            listOf(
                ContractFixtureFile("a/request/one.json", "{\"x\":1}"),
                ContractFixtureFile("b/request/two.json", "{\"y\":2}"),
            )

        val source = generatedContractFixturesSource(fixtures)

        assertEquals(source, generatedContractFixturesSource(fixtures))
        assertTrue(source.indexOf("a/request/one.json") < source.indexOf("b/request/two.json"))
        assertTrue(source.startsWith("package com.repz.app.core.network.contract"))
    }

    private fun writeFixture(
        relativePath: String,
        content: String,
    ) {
        val file = tempDirectory.resolve(relativePath)
        file.toFile().parentFile.mkdirs()
        file.writeText(content)
    }
}
