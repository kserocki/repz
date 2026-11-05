package com.repz.app.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material Symbols Rounded icon sizes (DESIGN_SYSTEM §8). Features never hard-code an icon size
 * value. */
object RepzIconSize {
    val dense: Dp = 20.dp
    val default: Dp = 24.dp
    val navBar: Dp = 28.dp

    /** The error icon inlined in a field's error text; not yet in DESIGN_SYSTEM §8's table. */
    val inline: Dp = 16.dp

    /** The large placeholder icon centred in an illustration backdrop (e.g. Welcome) until the live
     * illustration ships; not yet in DESIGN_SYSTEM §8's table. */
    val illustration: Dp = 64.dp
}
