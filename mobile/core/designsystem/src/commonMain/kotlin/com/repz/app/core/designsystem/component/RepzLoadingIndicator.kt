package com.repz.app.core.designsystem.component

import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The M3 Expressive shape-loop loading indicator (DESIGN_SYSTEM §9, board 06): the plain,
 * indeterminate `LoadingIndicator` (48 dp, `primary`) or, when [contained], `ContainedLoadingIndicator`
 * on `secondaryContainer`/`onSecondaryContainer` (the "on buttons/over content" variant). Never shown
 * on the live workout screen, which is local and instant (board 06); that screen uses a determinate
 * progress UI instead.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepzLoadingIndicator(
    contentDescription: String,
    modifier: Modifier = Modifier,
    contained: Boolean = false,
) {
    val indicatorModifier = modifier.progressSemantics().semantics { this.contentDescription = contentDescription }
    if (contained) {
        ContainedLoadingIndicator(
            modifier = indicatorModifier,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            indicatorColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    } else {
        LoadingIndicator(modifier = indicatorModifier)
    }
}
