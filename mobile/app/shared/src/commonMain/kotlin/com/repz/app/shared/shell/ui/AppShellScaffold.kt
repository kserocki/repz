package com.repz.app.shared.shell.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.component.RepzNavigationBar
import com.repz.app.core.designsystem.component.RepzNavigationBarItem
import com.repz.app.core.designsystem.theme.RepzTheme
import com.repz.app.core.navigation.TopLevelDestination
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The signed-in app shell: [RepzNavigationBar] with the 4 tabs, with [miniBar] rendered above it
 * on every tab except Train (always empty here; a later feature fills the slot in). Edge-to-edge
 * insets (including the iOS home-indicator inset) come from `Scaffold`'s and `NavigationBar`'s own
 * `WindowInsets.navigationBars` defaults.
 *
 * [selectedDestination] `null` is the signed-out auth flow: no nav bar, no mini bar. The gate in
 * `app:shared` decides which; this scaffold only renders what it is told. [isNavigationBarVisible]
 * `false` hides the whole bottom bar (nav bar and mini bar both) even with a destination selected,
 * for a full-screen destination like the live workout (T1, ADR-0029).
 */
@Composable
internal fun AppShellScaffold(
    selectedDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    isNavigationBarVisible: Boolean = true,
    miniBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (selectedDestination != null && isNavigationBarVisible) {
                Column {
                    if (selectedDestination != TopLevelDestination.Train) {
                        miniBar()
                    }
                    RepzNavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            RepzNavigationBarItem(
                                selected = destination == selectedDestination,
                                onClick = { onDestinationClick(destination) },
                                icon = painterResource(destination.icon),
                                selectedIcon = painterResource(destination.selectedIcon),
                                label = stringResource(destination.label),
                            )
                        }
                    }
                }
            }
        },
        content = content,
    )
}

@Preview
@Composable
private fun AppShellScaffoldSignedOutLightPreview() {
    RepzTheme(darkTheme = false) { AppShellScaffoldPreviewSample(selectedDestination = null) }
}

@Preview
@Composable
private fun AppShellScaffoldSignedOutDarkPreview() {
    RepzTheme(darkTheme = true) { AppShellScaffoldPreviewSample(selectedDestination = null) }
}

@Preview
@Composable
private fun AppShellScaffoldTrainLightPreview() {
    RepzTheme(darkTheme = false) {
        AppShellScaffoldPreviewSample(selectedDestination = TopLevelDestination.Train)
    }
}

@Preview
@Composable
private fun AppShellScaffoldTrainDarkPreview() {
    RepzTheme(darkTheme = true) {
        AppShellScaffoldPreviewSample(selectedDestination = TopLevelDestination.Train)
    }
}

@Preview
@Composable
private fun AppShellScaffoldHistoryLightPreview() {
    RepzTheme(darkTheme = false) {
        AppShellScaffoldPreviewSample(selectedDestination = TopLevelDestination.History)
    }
}

@Preview
@Composable
private fun AppShellScaffoldHistoryDarkPreview() {
    RepzTheme(darkTheme = true) {
        AppShellScaffoldPreviewSample(selectedDestination = TopLevelDestination.History)
    }
}

@Preview(locale = "pl")
@Composable
private fun AppShellScaffoldPolishPreview() {
    RepzTheme(darkTheme = false) {
        AppShellScaffoldPreviewSample(selectedDestination = TopLevelDestination.History)
    }
}

@Composable
private fun AppShellScaffoldPreviewSample(selectedDestination: TopLevelDestination?) {
    AppShellScaffold(
        selectedDestination = selectedDestination,
        onDestinationClick = {},
    ) { paddingValues ->
        val contentModifier = Modifier.padding(paddingValues)
        if (selectedDestination != null) {
            TabRootPlaceholder(destination = selectedDestination, modifier = contentModifier)
        } else {
            BrandBackground(modifier = contentModifier)
        }
    }
}
