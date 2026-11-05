package com.repz.app.core.designsystem.logo

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** [RepzLoader]'s 3 sizes (MOT-25): a minimum width, the artwork's own aspect ratio ([RepzLogoSymbol]
 * sizing) sets the height. */
enum class RepzLoaderSize(
    val width: Dp,
) {
    Small(24.dp),
    Medium(48.dp),
    Large(96.dp),
}
