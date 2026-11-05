package com.repz.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Board 07: 12 dp (RepzSpacing.md) vertical, 14 dp internal horizontal (an exact board measurement,
// not on the 4 dp grid; Q12 allows component-internal dimensions to stay exact).
private val BannerHorizontalPadding = 14.dp

/**
 * The quiet, non-error way to surface offline/warning state (DESIGN_SYSTEM §1 "Offline is normal",
 * §9, board 07): `RepzTheme.colors.warningContainer`/`onWarningContainer`, `shapes.large` (16).
 * A polite live region, so TalkBack/VoiceOver announce it when it appears.
 */
@Composable
fun RepzBanner(
    icon: RepzIcon,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    Row(
        modifier =
            modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .background(color = RepzTheme.colors.warningContainer, shape = MaterialTheme.shapes.large)
                .padding(horizontal = BannerHorizontalPadding, vertical = RepzSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        RepzIcon(
            icon = icon,
            contentDescription = null,
            modifier = Modifier.size(RepzIconSize.default),
            tint = RepzTheme.colors.onWarningContainer,
        )
        Column {
            Text(title, color = RepzTheme.colors.onWarningContainer, style = MaterialTheme.typography.titleSmall)
            if (body != null) {
                Text(body, color = RepzTheme.colors.onWarningContainer, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
