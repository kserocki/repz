package com.repz.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Board 07: 12 dp (RepzSpacing.md) vertical, 14 dp internal horizontal (an exact board measurement,
// not on the 4 dp grid; Q12 allows component-internal dimensions to stay exact).
private val BannerHorizontalPadding = 14.dp

// U6f, board 06: the neutral strip's own icon size and minimum height, neither of which is a shared
// RepzIconSize/RepzSpacing token (component-internal, Q12).
private val NeutralBannerIconSize = 18.dp
private val NeutralBannerMinHeight = 40.dp

/** The warning (default, unchanged since it originally landed) container/content colours and shape,
 * vs. the neutral "thin strip" ones (U6f, board 06/08 6b: a quiet offline notice, not an error). */
enum class RepzBannerStyle { Warning, Neutral }

private val RepzBannerStyle.containerColor: Color
    @Composable get() =
        when (this) {
            RepzBannerStyle.Warning -> RepzTheme.colors.warningContainer
            RepzBannerStyle.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
        }

private val RepzBannerStyle.contentColor: Color
    @Composable get() =
        when (this) {
            RepzBannerStyle.Warning -> RepzTheme.colors.onWarningContainer
            RepzBannerStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        }

private val RepzBannerStyle.iconTint: Color
    @Composable get() =
        when (this) {
            RepzBannerStyle.Warning -> RepzTheme.colors.onWarningContainer
            RepzBannerStyle.Neutral -> RepzTheme.colors.warning
        }

private val RepzBannerStyle.iconSize: Dp
    get() =
        when (this) {
            RepzBannerStyle.Warning -> RepzIconSize.default
            RepzBannerStyle.Neutral -> NeutralBannerIconSize
        }

private val RepzBannerStyle.shape: Shape
    @Composable get() =
        when (this) {
            RepzBannerStyle.Warning -> MaterialTheme.shapes.large
            RepzBannerStyle.Neutral -> MaterialTheme.shapes.medium
        }

private val RepzBannerStyle.titleStyle: TextStyle
    @Composable get() =
        when (this) {
            RepzBannerStyle.Warning -> MaterialTheme.typography.titleSmall
            RepzBannerStyle.Neutral -> MaterialTheme.typography.bodyMedium
        }

private val RepzBannerStyle.verticalAlignment: Alignment.Vertical
    get() =
        when (this) {
            RepzBannerStyle.Warning -> Alignment.Top
            RepzBannerStyle.Neutral -> Alignment.CenterVertically
        }

private val RepzBannerStyle.horizontalPadding: Dp
    get() =
        when (this) {
            RepzBannerStyle.Warning -> BannerHorizontalPadding
            RepzBannerStyle.Neutral -> RepzSpacing.md
        }

private val RepzBannerStyle.verticalPadding: Dp
    get() =
        when (this) {
            RepzBannerStyle.Warning -> RepzSpacing.md
            RepzBannerStyle.Neutral -> 0.dp
        }

private val RepzBannerStyle.gap: Dp
    get() =
        when (this) {
            RepzBannerStyle.Warning -> RepzSpacing.md
            RepzBannerStyle.Neutral -> RepzSpacing.sm
        }

private val RepzBannerStyle.minHeight: Dp?
    get() =
        when (this) {
            RepzBannerStyle.Warning -> null
            RepzBannerStyle.Neutral -> NeutralBannerMinHeight
        }

/**
 * The quiet, non-error way to surface offline/warning state (DESIGN_SYSTEM §1 "Offline is normal",
 * §9, boards 06/07/08): [RepzBannerStyle.Warning] (the default, unchanged) is
 * `RepzTheme.colors.warningContainer`/`onWarningContainer`, `shapes.large` (16);
 * [RepzBannerStyle.Neutral] is board 06's thin strip (`surfaceContainerHigh`/`onSurfaceVariant`,
 * `shapes.medium`, 40 dp min height, an 18 dp icon tinted `RepzTheme.colors.warning` — the icon
 * alone carries the hint of attention, the container stays neutral). A polite live region either
 * way, so TalkBack/VoiceOver announce it when it appears.
 */
@Composable
fun RepzBanner(
    icon: RepzIcon,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    style: RepzBannerStyle = RepzBannerStyle.Warning,
) {
    val minHeight = style.minHeight
    Row(
        modifier =
            modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .let { base -> if (minHeight != null) base.heightIn(min = minHeight) else base }
                .background(color = style.containerColor, shape = style.shape)
                .padding(horizontal = style.horizontalPadding, vertical = style.verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(style.gap),
        verticalAlignment = style.verticalAlignment,
    ) {
        RepzIcon(
            icon = icon,
            contentDescription = null,
            modifier = Modifier.size(style.iconSize),
            tint = style.iconTint,
        )
        Column {
            Text(title, color = style.contentColor, style = style.titleStyle)
            if (body != null) {
                Text(body, color = style.contentColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
