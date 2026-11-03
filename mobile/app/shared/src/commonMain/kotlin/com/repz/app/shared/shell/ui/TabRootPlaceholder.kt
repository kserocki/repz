package com.repz.app.shared.shell.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.navigation.TopLevelDestination
import org.jetbrains.compose.resources.stringResource

/**
 * The root screen of a tab that has no real screen yet: the tab's own label shown as a headline,
 * no new copy.
 */
@Composable
internal fun TabRootPlaceholder(
    destination: TopLevelDestination,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(destination.label),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun TabRootPlaceholderLightPreview() {
    RepzTheme(darkTheme = false) { TabRootPlaceholder(destination = TopLevelDestination.History) }
}

@Preview
@Composable
private fun TabRootPlaceholderDarkPreview() {
    RepzTheme(darkTheme = true) { TabRootPlaceholder(destination = TopLevelDestination.History) }
}
