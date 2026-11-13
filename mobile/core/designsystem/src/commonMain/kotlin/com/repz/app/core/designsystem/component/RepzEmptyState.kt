package com.repz.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzShapes
import com.repz.app.core.designsystem.theme.RepzSpacing

// Boards 11/12: the Cookie4 illustration backdrop, an exact board measurement.
private val EmptyStateBackdropSize = 150.dp

/**
 * The centred empty-state pattern (DESIGN_SYSTEM §9, boards 11/12): [illustration] centred on a
 * `cookie4Sided`/`surfaceContainerHigh` backdrop, then [title]/[body] (U6d:
 * `headlineSmallEmphasized`/`bodyLarge`, both centred and wrapping) and a caller-built [actions] slot
 * (e.g. a single `RepzHeroStartButton` or `RepzButton`). Only the centred layout ships here; board
 * 06's horizontal card variant waits for a screen that needs it.
 *
 * TODO(REP-49): MOT-23's idle loop on the illustration.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepzEmptyState(
    title: String,
    body: String,
    illustration: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        Box(
            modifier =
                Modifier
                    .size(EmptyStateBackdropSize)
                    .clip(RepzShapes.cookie4Sided.toShape())
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            illustration()
        }
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        actions()
    }
}
