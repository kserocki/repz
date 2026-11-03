package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.repz.app.core.designsystem.theme.RepzIconSize

/**
 * The 4-tab bottom navigation bar (DESIGN_SYSTEM §9 `RepzNavigationBar`): an M3 `NavigationBar`
 * with the [MaterialTheme.colorScheme.surfaceContainerLow] container the design calls for
 * (DESIGN_SYSTEM §7 "nav bar, header"). Height and the selection indicator stay at the M3 default.
 *
 * No selection animation here: the M3 default indicator and transitions apply as-is.
 * TODO(REP-48): MOT-02 (indicator stretch to 64×32, icon FILL 0→1, label weight 500→700, haptic).
 */
@Composable
fun RepzNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content,
    )
}

/**
 * One [RepzNavigationBar] tab: [selectedIcon] (FILL 1) while selected, [icon] (FILL 0) otherwise
 * (DESIGN_SYSTEM §8), [label] always shown below it. The icon is decorative
 * (`contentDescription = null`); [label] is the item's accessible name.
 */
@Composable
fun RowScope.RepzNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: Painter,
    selectedIcon: Painter,
    label: String,
    modifier: Modifier = Modifier,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                painter = if (selected) selectedIcon else icon,
                contentDescription = null,
                modifier = Modifier.size(RepzIconSize.navBar),
            )
        },
        modifier = modifier,
        label = { Text(label) },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(),
    )
}
