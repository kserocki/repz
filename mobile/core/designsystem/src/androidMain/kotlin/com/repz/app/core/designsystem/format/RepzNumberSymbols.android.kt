package com.repz.app.core.designsystem.format

import java.text.DecimalFormatSymbols
import java.util.Locale

internal actual fun platformNumberSymbols(): RepzNumberSymbols =
    RepzNumberSymbols(DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator)
