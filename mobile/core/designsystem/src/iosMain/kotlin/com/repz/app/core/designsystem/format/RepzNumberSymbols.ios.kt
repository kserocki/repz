package com.repz.app.core.designsystem.format

// TODO(REP-35): placeholder actual so core:designsystem compiles for iOS ahead of group 2.
// Swift's S1 (plan §3.3) replaces this with NSLocale.currentLocale.decimalSeparator's first
// character, falling back to '.'.
internal actual fun platformNumberSymbols(): RepzNumberSymbols = RepzNumberSymbols('.')
