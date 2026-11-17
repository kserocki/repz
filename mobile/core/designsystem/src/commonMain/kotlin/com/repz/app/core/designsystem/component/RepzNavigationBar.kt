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
 * The 4-tab bottom navigation bar (DESIGN_SYSTEM §9 `RepzNavigationBar`, board 04): an M3
 * `NavigationBar` with the [MaterialTheme.colorScheme.surfaceContainerLow] container the design
 * calls for (DESIGN_SYSTEM §7 "nav bar, header", U6a: board 04 draws `surfaceContainer`, kept as
 * `surfaceContainerLow` instead). Height (80 dp) stays the M3 default, which already matches
 * `RepzLayout.navigationBarHeight`.
 *
 * No selection animation here: the M3 default indicator transition applies as-is.
 * TODO(REP-48): MOT-02 (indicator leading edge spatialFast / trailing edge spatialDefault stretch to
 * 64×32, icon FILL 0→1 on effectsFast, Confirm haptic; reduced motion: indicator and icon jump).
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
 * (DESIGN_SYSTEM §8), [label] always shown below it in `labelMediumEmphasized` when selected or
 * `labelMedium` otherwise (U6b). The icon is decorative (`contentDescription = null`); [label] is
 * the item's accessible name. Colours (board 04): indicator `secondaryContainer`, selected icon
 * `onSecondaryContainer`, selected label `onSurface`, unselected icon/label `onSurfaceVariant`.
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
        label = {
            Text(label, style = if (selected) MaterialTheme.typography.labelMediumEmphasized else MaterialTheme.typography.labelMedium)
        },
        alwaysShowLabel = true,
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}
