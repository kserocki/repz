package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepzTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { if (title != null) Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    RepzIcon(icon = RepzIcon.ArrowBack, contentDescription = stringResource(Res.string.designsystem_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
