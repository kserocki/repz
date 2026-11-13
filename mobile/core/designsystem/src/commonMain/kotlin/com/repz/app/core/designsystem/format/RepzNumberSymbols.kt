package com.repz.app.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale

/**
 * The locale's decimal separator for formatting [FixedDecimal] values (ADR-0028). Set weights have
 * no grouping separator; a grouping symbol is added here if volumes ever need one.
 */
@Immutable
data class RepzNumberSymbols(
    val decimalSeparator: Char,
) {
    companion object {
        /** Previews only: real UI reads the platform locale through [platformNumberSymbols]. */
        val Default = RepzNumberSymbols('.')
    }
}

/**
 * The platform's locale decimal separator (ADR-0028): Android `DecimalFormatSymbols`, iOS
 * `NSLocale.currentLocale`. A `rememberRepzNumberSymbols()` composable wrapper, added with the
 * sheet, re-reads this when the composition's locale changes.
 */
internal expect fun platformNumberSymbols(): RepzNumberSymbols

/** Re-reads [platformNumberSymbols] whenever the composition's locale ([Locale.current]) changes. */
@Composable
fun rememberRepzNumberSymbols(): RepzNumberSymbols {
    val locale = Locale.current
    return remember(locale) { platformNumberSymbols() }
}
