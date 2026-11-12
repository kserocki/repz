package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.component.RepzLargeTopAppBar
import com.repz.app.core.designsystem.component.RepzMediumTopAppBar
import com.repz.app.core.designsystem.component.RepzNavigationBar
import com.repz.app.core.designsystem.component.RepzNavigationBarItem
import com.repz.app.core.designsystem.component.RepzTopAppBarScrollState
import com.repz.app.core.designsystem.component.rememberRepzTopAppBarScrollState
import com.repz.app.core.designsystem.component.repzTopAppBarScroll
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_exercise
import com.repz.app.core.designsystem.generated.resources.symbol_exercise_filled
import com.repz.app.core.designsystem.generated.resources.symbol_fitness_center
import com.repz.app.core.designsystem.generated.resources.symbol_fitness_center_filled
import com.repz.app.core.designsystem.generated.resources.symbol_history
import com.repz.app.core.designsystem.generated.resources.symbol_history_filled
import com.repz.app.core.designsystem.generated.resources.symbol_person
import com.repz.app.core.designsystem.generated.resources.symbol_person_filled
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// A demo viewport tall enough to leave visible list rows under the large bar (152 dp expanded)
// before it collapses to 64 dp, so swiping R12's collapse is easy to see; the page itself scrolls
// (plan §3.10), so this doesn't need to fit everything on one unscrolled screen.
private val TopAppBarDemoHeight = 220.dp
private const val DEMO_LIST_ITEM_COUNT = 20
private const val DEMO_LIST_ITEM_PREFIX = "Row "
private const val MEDIUM_BAR_TITLE = "Templates"
private const val LARGE_BAR_TITLE = "History"

private data class NavTab(
    val label: String,
    val icon: DrawableResource,
    val selectedIcon: DrawableResource,
)

private val NAV_TABS =
    listOf(
        NavTab("Train", Res.drawable.symbol_fitness_center, Res.drawable.symbol_fitness_center_filled),
        NavTab("History", Res.drawable.symbol_history, Res.drawable.symbol_history_filled),
        NavTab("Exercises", Res.drawable.symbol_exercise, Res.drawable.symbol_exercise_filled),
        NavTab("Profile", Res.drawable.symbol_person, Res.drawable.symbol_person_filled),
    )

/** `components-bars` (DESIGN_SYSTEM §9 `RepzNavigationBar`/`RepzMediumTopAppBar`/
 * `RepzLargeTopAppBar`): a tappable nav bar plus the medium and large top bars, each over its own
 * short scrollable list so swiping demonstrates the collapse-on-scroll (T5, R12). */
@Composable
internal fun ComponentsBarsCatalogPage() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("RepzNavigationBar")
        RepzNavigationBar {
            NAV_TABS.forEachIndexed { index, tab ->
                RepzNavigationBarItem(
                    selected = index == selectedTab,
                    onClick = { selectedTab = index },
                    icon = painterResource(tab.icon),
                    selectedIcon = painterResource(tab.selectedIcon),
                    label = tab.label,
                )
            }
        }

        CatalogSectionLabel("RepzMediumTopAppBar (scroll the list to collapse)")
        TopAppBarScrollDemo { scrollState ->
            RepzMediumTopAppBar(title = MEDIUM_BAR_TITLE, onBackClick = {}, scrollState = scrollState)
        }

        CatalogSectionLabel("RepzLargeTopAppBar (scroll the list to collapse)")
        TopAppBarScrollDemo { scrollState ->
            RepzLargeTopAppBar(title = LARGE_BAR_TITLE, onBackClick = {}, scrollState = scrollState)
        }
    }
}

// [scrollState]'s `nestedScroll` modifier must sit on an ancestor Box/Column of both the top app bar
// and its scrolling content (the same placement M3's own `Scaffold` sample uses), and the drag must
// start on the scrolling content itself — a touch that starts only on the bar has nothing to scroll.
@Composable
private fun TopAppBarScrollDemo(topAppBar: @Composable (RepzTopAppBarScrollState) -> Unit) {
    val scrollState = rememberRepzTopAppBarScrollState()
    Box(modifier = Modifier.fillMaxWidth().height(TopAppBarDemoHeight).repzTopAppBarScroll(scrollState)) {
        Column {
            topAppBar(scrollState)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(DEMO_LIST_ITEM_COUNT) { index ->
                    val rowLabel = "$DEMO_LIST_ITEM_PREFIX${index + 1}"
                    Text(
                        rowLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = RepzSpacing.lg, vertical = RepzSpacing.sm),
                    )
                }
            }
        }
    }
}
