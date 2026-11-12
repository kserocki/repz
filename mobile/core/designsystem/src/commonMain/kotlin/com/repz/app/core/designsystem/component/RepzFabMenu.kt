package com.repz.app.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.designsystem_close_menu
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMorphShape
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.motion.RepzStagger
import com.repz.app.core.designsystem.theme.RepzElevation
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

// Board 04 "FAB menu (open)": toggle grows from the 64 dp square into a 56 dp circle.
private val FabMenuOpenSize: Dp = 56.dp
private val FabMenuOpenCornerRadius: Dp = FabMenuOpenSize / 2
private const val FAB_MENU_OPEN_ROTATION_DEGREES = 45f

// Board 04: pill items, height 56, padding 16 start / 20 end, 10 dp icon-label gap.
private val FabMenuItemHeight: Dp = 56.dp
private val FabMenuItemStartPadding: Dp = 16.dp
private val FabMenuItemEndPadding: Dp = 20.dp
private val FabMenuItemGap: Dp = 10.dp

// MOT-30(c): items rise 8 dp while fading and scaling in from 0.8.
private val FabMenuItemRise: Dp = 8.dp
private const val FAB_MENU_ITEM_INITIAL_SCALE = 0.8f

/** One row of a [RepzFabMenu]: [onClick] fires before the menu closes. */
@Immutable
data class RepzFabMenuItem(
    val text: String,
    val icon: RepzIcon,
    val onClick: () -> Unit,
)

/**
 * The FAB that expands into a stack of actions (DESIGN_SYSTEM §9, board 04): MOT-30 end to end.
 * [items] render end-aligned above the toggle, top item first, each entering/leaving
 * [RepzMotion.stagger.listPerItem] apart — bottom-up on open, top-down on close (MOT-30c).
 * [collapsedContentDescription] names the toggle while collapsed (e.g. "New workout"); once
 * [expanded], it reads `designsystem_close_menu` instead. The menu closes on the toggle, on an
 * item, or on system Back while [expanded] (MOT-30f) — there is no scrim and no outside-tap
 * dismissal.
 *
 * Back uses the non-deprecated `androidx.navigationevent.compose.NavigationBackHandler`, not
 * `androidx.compose.ui.backhandler.BackHandler` (deprecated upstream in favour of this). On
 * Android, `androidx.navigationevent:navigationevent-compose-android` reads the dispatcher from
 * the `ComponentActivity`'s own `NavigationEventDispatcherOwner` via the view tree — `androidx
 * .activity` 1.13+ sets this up automatically in `ComponentActivity.onCreate()`, no app wiring
 * needed. On iOS it resolves through the JetBrains Compose Multiplatform port of the same library.
 * `rememberNavigationEventState` needs a `NavigationEventInfo`; [NavigationEventInfo.None] is the
 * library's own "no information needed" default, since this handler only cares about the gesture.
 */
@Composable
fun RepzFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    collapsedContentDescription: String,
    items: ImmutableList<RepzFabMenuItem>,
    modifier: Modifier = Modifier,
) {
    val closeMenuDescription = stringResource(Res.string.designsystem_close_menu)
    val density = LocalDensity.current
    val riseOffsetPx = with(density) { FabMenuItemRise.roundToPx() }
    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    NavigationBackHandler(state = backHandlerState, isBackEnabled = expanded, onBackCompleted = { onExpandedChange(false) })

    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        items.forEachIndexed { index, item ->
            key(index) {
                // Initialized from the current value, not always false, so a menu first composed
                // already expanded (e.g. restored state) renders its items immediately instead of
                // waiting out a stagger delay meant for a live open/close transition.
                var itemVisible by remember { mutableStateOf(expanded) }
                LaunchedEffect(expanded) {
                    delay(fabMenuItemEnterDelay(index = index, itemCount = items.size, opening = expanded))
                    itemVisible = expanded
                }
                AnimatedVisibility(
                    visible = itemVisible,
                    enter =
                        fadeIn(RepzMotion.effectsSprings.effectsFast()) +
                            scaleIn(RepzMotion.springs.spatialDefault(), initialScale = FAB_MENU_ITEM_INITIAL_SCALE) +
                            slideInVertically(RepzMotion.springs.spatialDefault()) { riseOffsetPx },
                    exit =
                        fadeOut(RepzMotion.effectsSprings.effectsFast()) +
                            scaleOut(RepzMotion.springs.spatialDefault(), targetScale = FAB_MENU_ITEM_INITIAL_SCALE) +
                            slideOutVertically(RepzMotion.springs.spatialDefault()) { riseOffsetPx },
                ) {
                    RepzFabMenuItemRow(item = item, onClick = {
                        item.onClick()
                        onExpandedChange(false)
                    })
                }
            }
        }

        RepzFabMenuToggle(
            expanded = expanded,
            onClick = { onExpandedChange(!expanded) },
            contentDescription = if (expanded) closeMenuDescription else collapsedContentDescription,
        )
    }
}

/** Bottom-up on open, top-down on close, [RepzStagger.listPerItem] apart: item `index` of
 * `itemCount` (0 = the item rendered at the top, closest to the caller's trigger, farthest from the
 * toggle) waits this long before it starts entering (`opening = true`) or leaving (`opening =
 * false`). */
internal fun fabMenuItemEnterDelay(
    index: Int,
    itemCount: Int,
    opening: Boolean,
): Duration {
    val position = if (opening) itemCount - 1 - index else index
    return RepzStagger.listPerItem * position
}

@Composable
private fun RepzFabMenuItemRow(
    item: RepzFabMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = FabMenuItemHeight),
        shape = RoundedCornerShape(FabMenuItemHeight / 2),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(start = FabMenuItemStartPadding, end = FabMenuItemEndPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FabMenuItemGap),
        ) {
            RepzIcon(icon = item.icon, contentDescription = null, modifier = Modifier.size(RepzIconSize.default))
            Text(item.text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * The FAB that doubles as open/close control (MOT-30b): 64 dp `largeIncreased` square
 * (`primaryContainer`/`onPrimaryContainer`) collapsed, 56 dp circle (`primary`/`onPrimary`)
 * expanded — a corner-radius animation on `spatialFast`, not a [RepzMorphShape]; colour crossfades
 * on `effectsFast`. The `+` glyph rotates 45° to read as `×` (no icon swap). While collapsed, a
 * press still morphs the square into Cookie4Sided and scales to 0.96 (MOT-30a); the two motions
 * never overlap in practice since a completed tap always releases the press before the menu opens.
 *
 * [cornerRadius] keeps driving the rendered shape — not just the toggle's own size — for as long as
 * it is still animating back down to [FabRestCornerRadius], even after [expanded] has already
 * flipped to `false`. Switching straight to [RepzMorphShape] the instant [expanded] goes false would
 * render that morph's `progress = 0f` start shape (a plain 20 dp square) immediately, throwing away
 * the circle → square animation in one frame. Once [cornerRadius] actually reaches
 * [FabRestCornerRadius], the two shapes coincide exactly, so the handover is seamless.
 */
@Composable
private fun RepzFabMenuToggle(
    expanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressed = isPressed && !expanded && !LocalReducedMotion.current
    val morph = rememberFabCookie4Morph()

    val pressProgress by animateFloatAsState(targetValue = if (pressed) 1f else 0f, animationSpec = RepzMotion.springs.pressMorph())
    val pressScale by
        animateFloatAsState(targetValue = if (pressed) FAB_PRESSED_SCALE else 1f, animationSpec = RepzMotion.springs.pressMorph())

    val containerSize by
        animateDpAsState(targetValue = if (expanded) FabMenuOpenSize else FabSize, animationSpec = RepzMotion.springs.spatialFast())
    val cornerRadius by
        animateDpAsState(
            targetValue = if (expanded) FabMenuOpenCornerRadius else FabRestCornerRadius,
            animationSpec = RepzMotion.springs.spatialFast(),
        )
    val rotation by
        animateFloatAsState(
            targetValue = if (expanded) FAB_MENU_OPEN_ROTATION_DEGREES else 0f,
            animationSpec = RepzMotion.springs.spatialFast(),
        )
    val containerColor by
        animateColorAsState(
            targetValue = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            animationSpec = RepzMotion.effectsSprings.effectsFast(),
        )
    val contentColor by
        animateColorAsState(
            targetValue = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            animationSpec = RepzMotion.effectsSprings.effectsFast(),
        )

    val isCollapsedAndSettled = !expanded && cornerRadius == FabRestCornerRadius
    val shape = if (isCollapsedAndSettled) RepzMorphShape(morph, pressProgress) else RoundedCornerShape(cornerRadius)
    val shadow = RepzElevation.fab.shadow

    Surface(
        onClick = onClick,
        modifier =
            modifier
                .size(containerSize)
                .scale(pressScale)
                .let { if (shadow != null) it.dropShadow(shape, shadow) else it },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RepzIcon(
                icon = RepzIcon.Add,
                contentDescription = contentDescription,
                modifier = Modifier.size(RepzIconSize.navBar).rotate(rotation),
            )
        }
    }
}
