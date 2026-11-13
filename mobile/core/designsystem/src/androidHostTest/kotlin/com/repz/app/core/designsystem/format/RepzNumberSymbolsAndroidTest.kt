package com.repz.app.core.designsystem.format

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [platformNumberSymbols] reads [Locale.getDefault], so these tests set and restore the JVM
 * default locale around each run rather than relying on the host's ambient locale.
 */
class RepzNumberSymbolsAndroidTest {
    private lateinit var originalDefaultLocale: Locale

    @BeforeTest
    fun saveDefaultLocale() {
        originalDefaultLocale = Locale.getDefault()
    }

    @AfterTest
    fun restoreDefaultLocale() {
        Locale.setDefault(originalDefaultLocale)
    }

    @Test
    fun givenUsDefaultLocale_whenReadingPlatformNumberSymbols_thenTheDecimalSeparatorIsADot() {
        Locale.setDefault(Locale.US)

        assertEquals('.', platformNumberSymbols().decimalSeparator)
    }

    @Test
    fun givenPolishDefaultLocale_whenReadingPlatformNumberSymbols_thenTheDecimalSeparatorIsAComma() {
        Locale.setDefault(
            Locale
                .Builder()
                .setLanguage("pl")
                .setRegion("PL")
                .build(),
        )

        assertEquals(',', platformNumberSymbols().decimalSeparator)
    }
}
