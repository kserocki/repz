package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.theme.RepzSpacing

// Board 04's own button labels, reused here as catalog sample copy only.
private fun buttonLabel(style: RepzButtonStyle) =
    when (style) {
        RepzButtonStyle.Filled -> "Add set"
        RepzButtonStyle.Tonal -> "Save template"
        RepzButtonStyle.Outlined -> "Keep training"
        RepzButtonStyle.Text -> "See all"
        RepzButtonStyle.Hero -> "Create account"
    }

/** `components-buttons` (DESIGN_SYSTEM §9 `RepzButton`): every style at size S in its
 * enabled/disabled/loading states, the XS/S/M size row, and a leading-icon example. */
@Composable
internal fun ComponentsButtonsCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        for (style in RepzButtonStyle.entries) {
            val label = buttonLabel(style)
            // TODO(REP-83): two rows keep each row at 2 labels, until iOS stops wrapping the 3rd
            // identical label one character per line.
            Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                    RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Small)
                    RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Small, enabled = false)
                }
                RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Small, isLoading = true)
            }
        }

        val sizeRowLabel = "Start"
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            RepzButton(sizeRowLabel, onClick = {}, size = RepzButtonSize.ExtraSmall)
            RepzButton(sizeRowLabel, onClick = {}, size = RepzButtonSize.Small)
        }
        RepzButton(sizeRowLabel, onClick = {}, size = RepzButtonSize.Medium)

        val iconLabel = "Add set"
        RepzButton(iconLabel, onClick = {}, style = RepzButtonStyle.Filled, leadingIcon = RepzIcon.Add)
    }
}
