package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMorphShape
import com.repz.app.core.designsystem.motion.RepzMorphs
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzElevation
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing

// DESIGN_SYSTEM §5/§9, board 04: FAB rest size 64 dp, largeIncreased (20 dp) corner radius. T1: the
// RepzMorphs press morph starts from this 20/64 rounded square (was 16 dp before this issue).
internal val FabSize: Dp = 64.dp
internal val FabRestCornerRadius: Dp = 20.dp

// MOT-30(a): press-and-hold morphs to Cookie4Sided and scales to 0.96, release springs back.
internal const val FAB_PRESSED_SCALE = 0.96f

// Board 04 "New workout" extended FAB.
private val ExtendedFabHeight = FabSize
private val ExtendedFabIconSize = 26.dp
private val ExtendedFabStartPadding = 18.dp
private val ExtendedFabEndPadding = 24.dp
private val ExtendedFabGap = 10.dp

/** The shared Square(20) → Cookie4Sided morph behind every FAB's press feedback (MOT-30a):
 * [RepzFab], [RepzExtendedFab] and the [RepzFabMenu] toggle button all drive the same shape family,
 * stretched to each control's own measured bounds by [RepzMorphShape]. */
@Composable
internal fun rememberFabCookie4Morph(): Morph = remember { RepzMorphs.roundedSquareToCookie4Sided(FabRestCornerRadius, FabSize) }

/**
 * A single-action floating action button (DESIGN_SYSTEM §9, board 04): 64 dp, `largeIncreased` (20
 * dp) square at rest, `primaryContainer`/`onPrimaryContainer`, the [RepzElevation.fab] shadow (light
 * theme only). MOT-30(a): press-and-hold morphs the square into `RepzShapes.cookie4Sided` and scales
 * to 0.96 on `pressMorph`, springing back on release; reduced motion keeps the state layer only
 * (MOT-22's rule).
 */
@Composable
fun RepzFab(
    icon: RepzIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && !LocalReducedMotion.current
    val morph = rememberFabCookie4Morph()
    val progress by animateFloatAsState(targetValue = if (morphed) 1f else 0f, animationSpec = RepzMotion.springs.pressMorph())
    val scale by animateFloatAsState(targetValue = if (morphed) FAB_PRESSED_SCALE else 1f, animationSpec = RepzMotion.springs.pressMorph())
    val shape = RepzMorphShape(morph, progress)
    val elevation = RepzElevation.fab

    Surface(
        onClick = onClick,
        modifier =
            modifier
                .size(FabSize)
                .scale(scale)
                .let { if (elevation.shadow != null) it.dropShadow(shape, elevation.shadow) else it },
        shape = shape,
        color = elevation.containerColor,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        interactionSource = interactionSource,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RepzIcon(icon = icon, contentDescription = contentDescription, modifier = Modifier.size(RepzIconSize.navBar))
        }
    }
}

/**
 * The labelled floating action button (DESIGN_SYSTEM §9, board 04 "New workout"): a minimum height
 * of 64 dp (so a long or PL label wraps instead of clipping, AGENTS.md §4), same `largeIncreased`
 * corner radius, elevation and MOT-30(a) press morph as [RepzFab].
 */
@Composable
fun RepzExtendedFab(
    text: String,
    icon: RepzIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && !LocalReducedMotion.current
    val morph = rememberFabCookie4Morph()
    val progress by animateFloatAsState(targetValue = if (morphed) 1f else 0f, animationSpec = RepzMotion.springs.pressMorph())
    val scale by animateFloatAsState(targetValue = if (morphed) FAB_PRESSED_SCALE else 1f, animationSpec = RepzMotion.springs.pressMorph())
    val shape = RepzMorphShape(morph, progress)
    val elevation = RepzElevation.fab

    Surface(
        onClick = onClick,
        modifier =
            modifier
                .heightIn(min = ExtendedFabHeight)
                .scale(scale)
                .let { if (elevation.shadow != null) it.dropShadow(shape, elevation.shadow) else it },
        shape = shape,
        color = elevation.containerColor,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(start = ExtendedFabStartPadding, end = ExtendedFabEndPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ExtendedFabGap),
        ) {
            RepzIcon(icon = icon, contentDescription = null, modifier = Modifier.size(ExtendedFabIconSize))
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}
