package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.theme.RepzColors
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

/** Every `RepzTheme.colors` token (DESIGN_SYSTEM §3.2): plain swatches for the tokens with no
 * on-colour pair, and a chip per semantic group (its container colour as the background, its
 * on-container colour as the text) for the x/onX/xContainer/onXContainer groups. */
@Composable
internal fun ExtendedCatalogPage() {
    val colors = RepzTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("brand / graphics")
        ColorSwatchGrid(entries = plainEntries(colors), columns = 3)

        CatalogSectionLabel("semantic groups (x on xContainer)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
            semanticGroups(colors).forEach { (label, container, onContainer) ->
                SemanticChip(label = label, containerColor = container, contentColor = onContainer)
            }
        }
    }
}

private fun plainEntries(colors: RepzColors) =
    listOf(
        ColorEntry("brandCyan", colors.brandCyan),
        ColorEntry("brandDeep", colors.brandDeep),
        ColorEntry("brandInk", colors.brandInk),
        ColorEntry("brandPaper", colors.brandPaper),
        ColorEntry("brandMist", colors.brandMist),
        ColorEntry("recordBright", colors.recordBright),
        ColorEntry("musclePrimary", colors.musclePrimary),
        ColorEntry("muscleSecondary", colors.muscleSecondary),
        ColorEntry("muscleInactive", colors.muscleInactive),
        ColorEntry("chartSeries1", colors.chartSeries1),
        ColorEntry("chartSeries2", colors.chartSeries2),
        ColorEntry("chartSeries3", colors.chartSeries3),
        ColorEntry("chartGrid", colors.chartGrid),
        ColorEntry("chartAxis", colors.chartAxis),
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
