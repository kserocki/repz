package com.repz.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzShapes

// Board 13 (profile header): the Cookie9 avatar is 68 dp, an exact board measurement, kept exact
// as a component-internal size rather than rounded to a value on the §6 spacing grid.
private val AvatarSize = 68.dp

/**
 * The account avatar (DESIGN_SYSTEM §5 Cookie9Sided, board 13): [monogram] (one uppercased code
 * point of the display name) on `primaryContainer`/`onPrimaryContainer`, the nearest emphasized
 * type token to the board's 28 px/850 weight (`headlineMediumEmphasized`). `null` renders an empty
 * `surfaceContainerHigh` placeholder, shown before any profile is cached. Decorative: the
 * parent row's name text already carries the accessible label.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepzAvatar(
    monogram: String?,
    modifier: Modifier = Modifier,
) {
    val shape = RepzShapes.cookie9Sided.toShape()
    val containerColor =
        if (monogram != null) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Box(
        modifier =
            modifier
                .size(AvatarSize)
                .clip(shape)
                .background(containerColor)
                .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (monogram != null) {
            Text(
                text = monogram,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
