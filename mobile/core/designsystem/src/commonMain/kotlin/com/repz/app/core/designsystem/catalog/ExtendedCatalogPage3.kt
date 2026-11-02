package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzColors
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/** Part 3 of 3 of every `RepzTheme.colors` token (DESIGN_SYSTEM §3.2): every semantic group's
 * `xContainer`/`onXContainer` colours, plus a chip per group showing the AA-contrast pair that's
 * actually drawn together (`onXContainer` text on the `xContainer` background). Part 1
 * ([ExtendedCatalogPage1]) has the tokens with no pair; part 2 ([ExtendedCatalogPage2]) has the
 * base `x`/`onX` colours. */
@Composable
internal fun ExtendedCatalogPage3() {
    val colors = RepzTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("semantic containers (xContainer / onXContainer)")
        ColorSwatchGrid(entries = semanticContainerEntries(colors), columns = 3)

        CatalogSectionLabel("contrast pairs (onXContainer on xContainer)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            semanticGroups(colors).forEach { (label, container, onContainer) ->
                SemanticChip(label = label, containerColor = container, contentColor = onContainer)
            }
        }
    }
}

private fun semanticContainerEntries(colors: RepzColors) =
    listOf(
        ColorEntry("successContainer", colors.successContainer),
        ColorEntry("onSuccessContainer", colors.onSuccessContainer),
        ColorEntry("warningContainer", colors.warningContainer),
        ColorEntry("onWarningContainer", colors.onWarningContainer),
        ColorEntry("recordContainer", colors.recordContainer),
        ColorEntry("onRecordContainer", colors.onRecordContainer),
        ColorEntry("setWarmupContainer", colors.setWarmupContainer),
        ColorEntry("onSetWarmupContainer", colors.onSetWarmupContainer),
        ColorEntry("setNormalContainer", colors.setNormalContainer),
        ColorEntry("onSetNormalContainer", colors.onSetNormalContainer),
        ColorEntry("setDropContainer", colors.setDropContainer),
        ColorEntry("onSetDropContainer", colors.onSetDropContainer),
        ColorEntry("setFailureContainer", colors.setFailureContainer),
        ColorEntry("onSetFailureContainer", colors.onSetFailureContainer),
    )

private fun semanticGroups(colors: RepzColors) =
    listOf(
        Triple("success", colors.successContainer, colors.onSuccessContainer),
        Triple("warning", colors.warningContainer, colors.onWarningContainer),
        Triple("record", colors.recordContainer, colors.onRecordContainer),
        Triple("setWarmup", colors.setWarmupContainer, colors.onSetWarmupContainer),
        Triple("setNormal", colors.setNormalContainer, colors.onSetNormalContainer),
        Triple("setDrop", colors.setDropContainer, colors.onSetDropContainer),
        Triple("setFailure", colors.setFailureContainer, colors.onSetFailureContainer),
    )
