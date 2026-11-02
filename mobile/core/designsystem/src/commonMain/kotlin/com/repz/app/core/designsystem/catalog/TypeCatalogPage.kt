package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.repz.app.core.designsystem.theme.RepzSpacing

/** The 15 baseline M3 styles (DESIGN_SYSTEM §4): sample text is the style's own name. Display and
 * headline samples may wrap. */
@Composable
internal fun TypeCatalogPage() {
    val typography = MaterialTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        TypeStyleRow("displayLarge", typography.displayLarge)
        TypeStyleRow("displayMedium", typography.displayMedium)
        TypeStyleRow("displaySmall", typography.displaySmall)
        TypeStyleRow("headlineLarge", typography.headlineLarge)
        TypeStyleRow("headlineMedium", typography.headlineMedium)
        TypeStyleRow("headlineSmall", typography.headlineSmall)
        TypeStyleRow("titleLarge", typography.titleLarge)
        TypeStyleRow("titleMedium", typography.titleMedium)
        TypeStyleRow("titleSmall", typography.titleSmall)
        TypeStyleRow("bodyLarge", typography.bodyLarge)
        TypeStyleRow("bodyMedium", typography.bodyMedium)
        TypeStyleRow("bodySmall", typography.bodySmall)
        TypeStyleRow("labelLarge", typography.labelLarge)
        TypeStyleRow("labelMedium", typography.labelMedium)
        TypeStyleRow("labelSmall", typography.labelSmall)
    }
}

@Composable
internal fun TypeStyleRow(
    name: String,
    style: TextStyle,
) {
    Text(
        text = name,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
}
