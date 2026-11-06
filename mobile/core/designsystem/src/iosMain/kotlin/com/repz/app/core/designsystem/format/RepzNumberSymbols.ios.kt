package com.repz.app.core.designsystem.format

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.decimalSeparator

/**
 * The current locale's decimal separator (ADR-0028), read from `NSLocale.currentLocale`. Falls
 * back to `.` if the locale reports an empty separator.
 */
internal actual fun platformNumberSymbols(): RepzNumberSymbols {
    val separator = NSLocale.currentLocale.decimalSeparator.firstOrNull() ?: '.'
    return RepzNumberSymbols(separator)
}
