package com.repz.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Board 07, component-internal (Q12: stays exact, not on the 4 dp spacing grid).
private val ActiveDotWidth = 24.dp
private val DotHeight = 8.dp
private val InactiveDotWidth = 8.dp
private val DotCornerRadius = 4.dp
private val DotGap = 6.dp

/**
 * The Welcome pager's dots (DESIGN_SYSTEM §9, board 07): active 24×8 `primary`, inactive 8×8
 * `outlineVariant`, radius 4, gap 6. No animation: no MOT entry exists yet for the pager itself. The
 * dots carry no semantics of their own; [contentDescription] (e.g. "Page 2 of 3") becomes the whole
 * row's merged description.
 */
@Composable
fun RepzPageIndicator(
    pageCount: Int,
    currentPage: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(DotGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (page in 0 until pageCount) {
            val isActive = page == currentPage
            val width: Dp = if (isActive) ActiveDotWidth else InactiveDotWidth
            val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier =
                    Modifier
                        .size(width = width, height = DotHeight)
                        .background(color = color, shape = RoundedCornerShape(DotCornerRadius)),
            )
        }
    }
}
