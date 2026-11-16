package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Board 04 §21: the check glyph inside the "on" thumb, same as M3's own SwitchDefaults.IconSize.
private val SwitchCheckIconSize = 16.dp

/**
 * The on/off control (DESIGN_SYSTEM §9, board 04 §21): M3's own 52×32 [Switch] with its default
 * colours (`primary` track / `onPrimary` thumb when checked, M3 semantics `Role.Switch`). A 16 dp
 * [RepzIcon.Check] tinted `primary` fills the thumb only while [checked] (board 04); the "off" thumb
 * stays plain, M3's default. [onCheckedChange] `null` makes the switch read-only, for a caller that
 * toggles it from the whole row instead (M3's own convention).
 */
@Composable
fun RepzSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent =
            if (checked) {
                {
                    RepzIcon(
                        icon = RepzIcon.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchCheckIconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
    )
}
