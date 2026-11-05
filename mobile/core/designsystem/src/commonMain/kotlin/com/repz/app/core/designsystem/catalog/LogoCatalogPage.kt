package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.RepzBrandColors
import com.repz.app.core.designsystem.logo.RepzLaunchLogo
import com.repz.app.core.designsystem.logo.RepzLaunchLogoPlayback
import com.repz.app.core.designsystem.logo.RepzLogoSymbol
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val StaticSymbolWidths = listOf(24.dp, 48.dp, 96.dp, 144.dp)
private const val LAUNCH_REPLAY_INTERVAL_MILLIS = 3000L

/** `logo` (brand/LOGO.md §2, MOT-01): the static symbol at every minimum-and-up size in both
 * colourways, then [RepzLaunchLogo] replaying on a loop so Spotter can review MOT-01 without a cold
 * start. */
@Composable
internal fun LogoCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("static symbol — brandDeep")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
            StaticSymbolWidths.forEach { width ->
                RepzLogoSymbol(modifier = Modifier.width(width), color = RepzBrandColors.Deep, contentDescription = null)
            }
        }

        CatalogSectionLabel("static symbol — brandCyan")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md)) {
            StaticSymbolWidths.forEach { width ->
                RepzLogoSymbol(modifier = Modifier.width(width), color = RepzBrandColors.Cyan, contentDescription = null)
            }
        }

        CatalogSectionLabel("RepzLaunchLogo (MOT-01, replays every 3 s)")
        ReplayingLaunchLogo()
    }
}

/** Remounts [RepzLaunchLogo] every [LAUNCH_REPLAY_INTERVAL_MILLIS] (`key` forces a fresh instance,
 * so its once-only choreography plays again), since `simctl`/instrumentation can't tap to replay. */
@Composable
private fun ReplayingLaunchLogo() {
    var replayKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(LAUNCH_REPLAY_INTERVAL_MILLIS)
            replayKey++
        }
    }
    key(replayKey) {
        RepzLaunchLogo(playback = RepzLaunchLogoPlayback.LoadTheBar, onFinish = {})
    }
}
