package com.repz.app.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_arrow_back
import com.repz.app.core.designsystem.generated.resources.symbol_cloud_off
import com.repz.app.core.designsystem.generated.resources.symbol_error
import com.repz.app.core.designsystem.generated.resources.symbol_visibility
import com.repz.app.core.designsystem.generated.resources.symbol_visibility_off
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** The auth subset of Material Symbols Rounded (DESIGN_SYSTEM §8); the 4 nav-bar icons ship
 * separately and the remaining icon set arrives later. */
enum class RepzIcon {
    ArrowBack,
    Visibility,
    VisibilityOff,
    Error,
    CloudOff,
}

/** One Material Symbols Rounded icon at its intrinsic 24 dp size unless [modifier] overrides it
 * (e.g. the 16 dp inline error icon, [com.repz.app.core.designsystem.theme.RepzIconSize.inline]).
 * [contentDescription] is `null` for a purely decorative icon. */
@Composable
fun RepzIcon(
    icon: RepzIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(icon.drawableResource),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

private val RepzIcon.drawableResource: DrawableResource
    get() =
        when (this) {
            RepzIcon.ArrowBack -> Res.drawable.symbol_arrow_back
            RepzIcon.Visibility -> Res.drawable.symbol_visibility
            RepzIcon.VisibilityOff -> Res.drawable.symbol_visibility_off
            RepzIcon.Error -> Res.drawable.symbol_error
            RepzIcon.CloudOff -> Res.drawable.symbol_cloud_off
        }
