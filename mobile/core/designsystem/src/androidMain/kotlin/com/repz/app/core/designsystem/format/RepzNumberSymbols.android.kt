package com.repz.app.core.designsystem.format

// TODO(REP-35): placeholder actual so core:designsystem compiles for Android ahead of group 2.
// Bolt's B1 (plan §3.3) replaces this with DecimalFormatSymbols.getInstance(Locale.getDefault())
// .decimalSeparator.
internal actual fun platformNumberSymbols(): RepzNumberSymbols = RepzNumberSymbols('.')
