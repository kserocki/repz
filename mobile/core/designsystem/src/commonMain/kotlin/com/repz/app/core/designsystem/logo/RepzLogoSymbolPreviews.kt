package com.repz.app.core.designsystem.logo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

// Preview-only specimen text (not user-facing): AGENTS.md §4 Strings covers product copy, not a
// design-system preview's own label.
private const val PREVIEW_ASSEMBLED_DESCRIPTION = "Assembled"
private const val PREVIEW_BAR_ONLY_DESCRIPTION = "Bar only"

@Composable
private fun RepzLogoSymbolShowcase() {
    Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzLogoSymbol(
            modifier = Modifier.width(96.dp),
            state = rememberRepzLogoSymbolState(RepzLogoPose.Assembled),
            contentDescription = PREVIEW_ASSEMBLED_DESCRIPTION,
        )
        RepzLogoSymbol(
            modifier = Modifier.width(96.dp),
            state = rememberRepzLogoSymbolState(RepzLogoPose.BarOnly),
            contentDescription = PREVIEW_BAR_ONLY_DESCRIPTION,
        )
    }
}

@Preview
@Composable
private fun RepzLogoSymbolLightPreview() {
    RepzTheme(darkTheme = false) { RepzLogoSymbolShowcase() }
}

@Preview
@Composable
private fun RepzLogoSymbolDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLogoSymbolShowcase() }
}
