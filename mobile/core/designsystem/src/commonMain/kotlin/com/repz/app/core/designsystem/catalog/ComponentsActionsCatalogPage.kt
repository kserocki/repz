package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.repz.app.core.designsystem.component.RepzPageIndicator
import com.repz.app.core.designsystem.component.RepzTopAppBar
import com.repz.app.core.designsystem.theme.RepzSpacing

private const val WELCOME_PAGE_COUNT = 3

/** `components-actions` (DESIGN_SYSTEM §9 `RepzTopAppBar`/`RepzPageIndicator`): a top bar with and
 * without a back action, and the page indicator at every page. The button matrix moved to
 * `components-buttons`/`components-buttons-large` (§3.10). */
@Composable
internal fun ComponentsActionsCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        val screenTitle = "Create your account"
        RepzTopAppBar()
        RepzTopAppBar(title = screenTitle)
        RepzTopAppBar(title = screenTitle, onBackClick = {})

        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            for (page in 0 until WELCOME_PAGE_COUNT) {
                val description = "Page ${page + 1} of $WELCOME_PAGE_COUNT"
                RepzPageIndicator(pageCount = WELCOME_PAGE_COUNT, currentPage = page, contentDescription = description)
            }
        }
    }
}
