package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// DESIGN_SYSTEM §6: "The live workout stays single-pane, centred, max 600 dp" on wide windows; a
// component-internal size, not on the §6 spacing/shape token scale.
private val MaxContentWidth = 600.dp

/**
 * The live-workout screen's outer frame (DESIGN_SYSTEM §6/§9): [header] and [bottomBar] pin to the
 * screen edges exactly like a standard `Scaffold`, [content] is measured between them and, on wide
 * windows, capped at [MaxContentWidth] and centred horizontally. [bottomBar] keeps a leading slot
 * reserved for a future rest pill; this scaffold doesn't draw one itself.
 */
@Composable
fun RepzLiveWorkoutScaffold(
    header: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = header,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
                content(innerPadding)
            }
        }
    }
}
