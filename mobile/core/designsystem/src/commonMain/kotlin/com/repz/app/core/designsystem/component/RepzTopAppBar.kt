package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.designsystem_back
import org.jetbrains.compose.resources.stringResource

/**
 * The small top app bar (DESIGN_SYSTEM §6/§9, board 07): height 64, `surface` container, `titleLarge`
 * title (M3's own default for a small [TopAppBar]), status-bar insets applied (M3's default
 * [TopAppBarDefaults.windowInsets]). `onBackClick == null` omits the navigation icon entirely.
 *
 * [backButtonModifier] reaches the back button itself (e.g. a feature's `Modifier.testTag(...)`),
 * since [modifier] lands on the whole bar and a tag there would report the full-width bar's bounds
 * to UiAutomator/XCUITest instead of the small tappable icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepzTopAppBar(
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { if (title != null) Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick, modifier = backButtonModifier) {
                    RepzIcon(icon = RepzIcon.ArrowBack, contentDescription = stringResource(Res.string.designsystem_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

/**
 * The medium top app bar (DESIGN_SYSTEM §6/§9, T5): 112 dp expanded, collapsing to the same 64 dp
 * small-bar height as [scrollState] scrolls (M3's own `exitUntilCollapsed` defaults already match
 * `RepzLayout.topAppBarMediumHeight`/`topAppBarSmallHeight`). `surface` container, `headlineSmall`
 * title (M3's own default for this bar). A `null` [scrollState] renders a non-collapsing bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepzMediumTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollState: RepzTopAppBarScrollState? = null,
) {
    MediumTopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick, modifier = backButtonModifier) {
                    RepzIcon(icon = RepzIcon.ArrowBack, contentDescription = stringResource(Res.string.designsystem_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        scrollBehavior = scrollState?.behavior,
    )
}

/**
 * The large top app bar (DESIGN_SYSTEM §6/§9, T5): 152 dp expanded, collapsing to 64 dp as
 * [scrollState] scrolls, `headlineMedium` title (M3's own default). Same parameters and container
 * colour as [RepzMediumTopAppBar], only the expanded height and title size differ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepzLargeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollState: RepzTopAppBarScrollState? = null,
) {
    LargeTopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick, modifier = backButtonModifier) {
                    RepzIcon(icon = RepzIcon.ArrowBack, contentDescription = stringResource(Res.string.designsystem_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        scrollBehavior = scrollState?.behavior,
    )
}
