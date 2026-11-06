package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzTheme

// Boards 04/08/18 "Start workout" hero (component-internal, exact). Internal (not private) so
// RepzHeroStartButtonSpecTest can assert them directly against DESIGN_SYSTEM §9/§3.2.
internal val HeroHeight = 88.dp
internal val HeroDiscSize = 56.dp
internal val HeroDiscPressedRadius = 16.dp
internal val HeroPressedRadius = 22.dp
private val HeroIconSize = 32.dp
private val HeroStartPadding = 20.dp
private val HeroEndPadding = 32.dp
private val HeroDiscLabelGap = 14.dp

// Board 08 split variant.
private val HeroSplitPadding = 16.dp
private val HeroSplitDiscLabelGap = 12.dp
private val HeroSplitSegmentGap = 2.dp
internal val HeroSplitInnerRadius = 12.dp
private val HeroSplitTrailingWidth = 64.dp

private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val PRESSED_SCALE = 0.96f

// T11: the disc is onPrimaryContainer (brandInk) at 12 % alpha, the same number as the disabled-
// container rule below but otherwise unrelated to it; the disc never re-tints when the whole button
// is disabled.
private const val DISC_BACKGROUND_ALPHA = 0.12f

/**
 * The "Start workout" CTA (DESIGN_SYSTEM §9 `HeroStartButton`, boards 04/08/18): `primaryContainer`/
 * `onPrimaryContainer`, [HeroHeight] minimum height (grows when [text] wraps), full pill at rest, a
 * [HeroDiscSize] icon disc. [text] is set in `RepzTheme.typography.heroLabel` (U1) and shares the
 * row with the disc via `weight(1f)`, so it reads as centred when the button is wider than its
 * natural content and still wraps, never truncates, when it isn't.
 *
 * MOT-22/MOT-03, press stage only: the container morphs from the rest pill to
 * [HeroPressedRadius] (22 dp) and the disc's circle morphs to a [HeroDiscPressedRadius] (16 dp)
 * squircle, both scaling to 0.96 on [RepzMotion.springs.pressMorph]. Reduced motion: state layer
 * only. No haptic here — the caller fires Confirm only when the workout actually starts.
 *
 * TODO(REP-48): the container-transform-into-the-live-screen stage of MOT-03 (icon swap to a
 * loading dumbbell, container growing into the live workout screen) is not built here.
 */
@Composable
fun RepzHeroStartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && !LocalReducedMotion.current

    val containerRadius by
        animateDpAsState(
            targetValue = if (morphed) HeroPressedRadius else HeroHeight / 2,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val discRadius by
        animateDpAsState(
            targetValue = if (morphed) HeroDiscPressedRadius else HeroDiscSize / 2,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val scale by
        animateFloatAsState(
            targetValue = if (morphed) PRESSED_SCALE else 1f,
            animationSpec = RepzMotion.springs.pressMorph(),
        )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(containerRadius),
        color = heroContainerColor(enabled),
        contentColor = heroContentColor(enabled),
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = HeroHeight).scale(scale).semantics { role = Role.Button },
    ) {
        Row(
            modifier = Modifier.padding(start = HeroStartPadding, end = HeroEndPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepzHeroDisc(cornerRadius = discRadius)
            Spacer(Modifier.width(HeroDiscLabelGap))
            Text(text, style = RepzTheme.typography.heroLabel, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * The split variant (DESIGN_SYSTEM §9, board 08): [onClick] (main segment, disc + [text]) starts an
 * empty workout; [onSecondaryClick] (trailing [HeroSplitTrailingWidth] segment, `ListAlt`) starts
 * from a template. [secondaryContentDescription] is the trailing segment's accessible label (e.g.
 * "Start from template"), since it carries no visible text.
 *
 * U2(g): only the pressed segment morphs — its outer corners go full → [HeroPressedRadius] and it
 * scales to 0.96; the [HeroSplitInnerRadius] corners shared between the two segments never move.
 */
@Composable
fun RepzHeroStartSplitButton(
    text: String,
    onClick: () -> Unit,
    secondaryContentDescription: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val mainInteractionSource = remember { MutableInteractionSource() }
    val secondaryInteractionSource = remember { MutableInteractionSource() }
    val restOuterRadius = HeroHeight / 2

    Row(
        modifier = modifier.heightIn(min = HeroHeight),
        horizontalArrangement = Arrangement.spacedBy(HeroSplitSegmentGap),
    ) {
        RepzHeroMainSegment(
            text = text,
            onClick = onClick,
            enabled = enabled,
            interactionSource = mainInteractionSource,
            restOuterRadius = restOuterRadius,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        RepzHeroSecondarySegment(
            secondaryContentDescription = secondaryContentDescription,
            onClick = onSecondaryClick,
            enabled = enabled,
            interactionSource = secondaryInteractionSource,
            restOuterRadius = restOuterRadius,
            modifier = Modifier.width(HeroSplitTrailingWidth).fillMaxHeight(),
        )
    }
}

@Composable
private fun RepzHeroMainSegment(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    restOuterRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && !LocalReducedMotion.current

    val outerRadius by
        animateDpAsState(
            targetValue = if (morphed) HeroPressedRadius else restOuterRadius,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val discRadius by
        animateDpAsState(
            targetValue = if (morphed) HeroDiscPressedRadius else HeroDiscSize / 2,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val scale by
        animateFloatAsState(
            targetValue = if (morphed) PRESSED_SCALE else 1f,
            animationSpec = RepzMotion.springs.pressMorph(),
        )

    val corners =
        connectedGroupSegmentCorners(isFirst = true, isLast = false, outerRadius = outerRadius, innerRadius = HeroSplitInnerRadius)
    val shape = RoundedCornerShape(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = heroContainerColor(enabled),
        contentColor = heroContentColor(enabled),
        interactionSource = interactionSource,
        modifier = modifier.scale(scale).semantics { role = Role.Button },
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = HeroSplitPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepzHeroDisc(cornerRadius = discRadius)
            Spacer(Modifier.width(HeroSplitDiscLabelGap))
            Text(text, style = RepzTheme.typography.heroLabel)
        }
    }
}

@Composable
private fun RepzHeroSecondarySegment(
    secondaryContentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    restOuterRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && !LocalReducedMotion.current

    val outerRadius by
        animateDpAsState(
            targetValue = if (morphed) HeroPressedRadius else restOuterRadius,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val scale by
        animateFloatAsState(
            targetValue = if (morphed) PRESSED_SCALE else 1f,
            animationSpec = RepzMotion.springs.pressMorph(),
        )

    val corners =
        connectedGroupSegmentCorners(isFirst = false, isLast = true, outerRadius = outerRadius, innerRadius = HeroSplitInnerRadius)
    val shape = RoundedCornerShape(corners.topStart, corners.topEnd, corners.bottomEnd, corners.bottomStart)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = heroContainerColor(enabled),
        contentColor = heroContentColor(enabled),
        interactionSource = interactionSource,
        modifier =
            modifier.scale(scale).semantics {
                this.contentDescription = secondaryContentDescription
                role = Role.Button
            },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RepzIcon(RepzIcon.ListAlt, contentDescription = null, modifier = Modifier.size(RepzIconSize.navBar))
        }
    }
}

@Composable
private fun RepzHeroDisc(
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(HeroDiscSize)
                .background(
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = DISC_BACKGROUND_ALPHA),
                    RoundedCornerShape(cornerRadius),
                ),
        contentAlignment = Alignment.Center,
    ) {
        RepzIcon(
            icon = RepzIcon.PlayArrowFilled,
            contentDescription = null,
            modifier = Modifier.size(HeroIconSize),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun heroContainerColor(enabled: Boolean): Color =
    if (enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    }

@Composable
private fun heroContentColor(enabled: Boolean): Color =
    if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    }
