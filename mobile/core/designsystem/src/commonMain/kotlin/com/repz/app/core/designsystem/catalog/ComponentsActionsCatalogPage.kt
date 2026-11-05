package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzPageIndicator
import com.repz.app.core.designsystem.component.RepzTopAppBar
import com.repz.app.core.designsystem.theme.RepzSpacing

private const val WELCOME_PAGE_COUNT = 3

// Board 04's own button labels, reused here as catalog sample copy only.
private fun buttonLabel(style: RepzButtonStyle) =
    when (style) {
        RepzButtonStyle.Filled -> "Add set"
        RepzButtonStyle.Outlined -> "Keep training"
        RepzButtonStyle.Text -> "See all"
    }

/** `components-actions` (DESIGN_SYSTEM §9 `RepzButton`/`RepzTopAppBar`/`RepzPageIndicator`): every
 * button style × size × enabled/disabled/loading, a top bar with and without a back action, and the
 * page indicator at every page. */
@Composable
internal fun ComponentsActionsCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        for (style in RepzButtonStyle.entries) {
            val label = buttonLabel(style)
            // TODO(REP-83): back to one row of four once iOS stops wrapping the 3rd identical
            // label in a row one character per line; two rows keep each row at 2 labels.
            Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                    RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Small)
                    RepzButton(label, onClick = {}, style = style, size = RepzButtonSize.Medium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
                    RepzButton(label, onClick = {}, style = style, enabled = false)
                    RepzButton(label, onClick = {}, style = style, isLoading = true)
                }
            }
        }

        val screenTitle = "Create your account"
        RepzTopAppBar()
        RepzTopAppBar(title = screenTitle)
        RepzTopAppBar(title = screenTitle, onBackClick = {})

        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            for (page in 0 until WELCOME_PAGE_COUNT) {
                val description = "Page ${page + 1} of $WELCOME_PAGE_COUNT"
                RepzPageIndicator(pageCount = WELCOME_PAGE_COUNT, currentPage = page, contentDescription = description)
            }
        }
    }
}
