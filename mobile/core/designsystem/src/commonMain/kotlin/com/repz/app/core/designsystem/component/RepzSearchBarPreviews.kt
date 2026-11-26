package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzSearchBarShowcase() {
    val searchPlaceholder = "Search exercises"
    val clearDescription = "Clear search"
    val closeDescription = "Close picker"

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        RepzSearchBar(query = "", onQueryChange = {}, placeholder = searchPlaceholder, clearContentDescription = clearDescription)
        RepzSearchBar(
            query = "Bench press",
            onQueryChange = {},
            placeholder = searchPlaceholder,
            clearContentDescription = clearDescription,
        )
        RepzSearchBar(
            query = "",
            onQueryChange = {},
            placeholder = searchPlaceholder,
            clearContentDescription = clearDescription,
            leadingIcon = RepzIcon.Close,
            onLeadingClick = {},
            leadingContentDescription = closeDescription,
        )
    }
}

@Preview
@Composable
private fun RepzSearchBarLightPreview() {
    RepzTheme(darkTheme = false) { RepzSearchBarShowcase() }
}

@Preview
@Composable
private fun RepzSearchBarDarkPreview() {
    RepzTheme(darkTheme = true) { RepzSearchBarShowcase() }
}

// 200%: the placeholder wraps onto a 2nd line instead of clipping, and the bar grows with it.
@Preview(fontScale = 2f)
@Composable
private fun RepzSearchBarFontScale200LightPreview() {
    RepzTheme(darkTheme = false) {
        RepzSearchBar(
            query = "",
            onQueryChange = {},
            placeholder = "A placeholder long enough to wrap onto a second line at this width",
            clearContentDescription = "Clear search",
        )
    }
}

@Preview(fontScale = 2f)
@Composable
private fun RepzSearchBarFontScale200DarkPreview() {
    RepzTheme(darkTheme = true) {
        RepzSearchBar(
            query = "",
            onQueryChange = {},
            placeholder = "A placeholder long enough to wrap onto a second line at this width",
            clearContentDescription = "Clear search",
        )
    }
}
