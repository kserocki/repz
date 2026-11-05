package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.motion.LocalReducedMotion
import com.repz.app.core.designsystem.motion.RepzMotion

enum class RepzButtonStyle { Filled, Outlined, Text }

enum class RepzButtonSize { Small, Medium }

// DESIGN_SYSTEM §9 "RepzButton" (XS/L/XL ship later): S 40, M 56.
private val SmallHeight = 40.dp
private val MediumHeight = 56.dp

// DESIGN_SYSTEM §5 "medium" (12 dp, "pressed buttons"): the shared pressed corner radius (MOT-22).
private val PressedCornerRadius = 12.dp

// DESIGN_SYSTEM §9 "disabled onSurface 12 % container / 38 % content" (board 04).
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

// MOT-22 press feedback (MOTION.md): scale to 0.96 on press.
private const val PRESSED_BUTTON_SCALE = 0.96f

private val OutlinedButtonBorderWidth = 1.dp

private val RepzButtonSize.height: Dp
    get() =
        when (this) {
            RepzButtonSize.Small -> SmallHeight
            RepzButtonSize.Medium -> MediumHeight
        }

// A rest-state corner radius of half the height renders the same stadium (pill) shape as
// RepzShapes.full (CircleShape), but as a Dp so it can animate smoothly toward PressedCornerRadius.
private val RepzButtonSize.restCornerRadius: Dp
    get() = height / 2

private val RepzButtonSize.contentPadding: PaddingValues
    get() =
        when (this) {
            RepzButtonSize.Small -> ButtonDefaults.SmallContentPadding
            RepzButtonSize.Medium -> ButtonDefaults.MediumContentPadding
        }

// Q12: titleMedium is the nearest token to board 07's Medium button label size; labelLarge is S's
// own token already.
private val RepzButtonSize.labelStyle: TextStyle
    @Composable get() =
        when (this) {
            RepzButtonSize.Small -> MaterialTheme.typography.labelLarge
            RepzButtonSize.Medium -> MaterialTheme.typography.titleMedium
        }

private val RepzButtonSize.loadingIndicatorSize: Dp
    get() =
        when (this) {
            RepzButtonSize.Small -> ButtonDefaults.SmallIconSize
            RepzButtonSize.Medium -> ButtonDefaults.MediumIconSize
        }

/**
 * The primary action control (DESIGN_SYSTEM §9, board 04): [RepzButtonStyle.Filled] (high
 * emphasis, `primary`/`onPrimary`; the decided CTA colour for every call to action, recorded in
 * DESIGN_SYSTEM §13), [RepzButtonStyle.Outlined] (medium emphasis, `outline` border) or
 * [RepzButtonStyle.Text] (low emphasis). [RepzButtonSize.Small] (40 dp) or
 * [RepzButtonSize.Medium] (56 dp) is a minimum height: at larger font scales [text] wraps to more
 * lines and the button grows to fit, never clipping or truncating. XS/L/XL ship later.
 *
 * MOT-22 press feedback: the shape morphs from the rest pill to [PressedCornerRadius] and scales to
 * 0.96, both on [RepzMotion.springs.pressMorph]. With reduced motion, neither animates: only the
 * default state layer (ripple) shows, per MOTION.md's "Reduced motion: Ripple/state layer only".
 *
 * [isLoading] keeps the button's measured size and ignores clicks, but keeps the enabled colours so
 * it never looks disabled; [loadingContentDescription] becomes the accessible description while the
 * M3 Expressive loading indicator replaces [text].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepzButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RepzButtonStyle = RepzButtonStyle.Filled,
    size: RepzButtonSize = RepzButtonSize.Medium,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingContentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val morphed = isPressed && !isLoading && !LocalReducedMotion.current

    val cornerRadius by
        animateDpAsState(
            targetValue = if (morphed) PressedCornerRadius else size.restCornerRadius,
            animationSpec = RepzMotion.springs.pressMorph(),
        )
    val scale by
        animateFloatAsState(
            targetValue = if (morphed) PRESSED_BUTTON_SCALE else 1f,
            animationSpec = RepzMotion.springs.pressMorph(),
        )

    // True only when genuinely disabled (not while loading), so a loading button keeps its normal
    // colours even though `enabled` below is false and it stops responding to clicks.
    val isVisuallyDisabled = !enabled && !isLoading

    Button(
        onClick = onClick,
        // A minimum, not a fixed, height: at large font scales the label wraps to more lines and the
        // button grows to fit it instead of clipping (AGENTS.md §4, AC #7 "buttons wrap, never
        // truncate").
        modifier = modifier.heightIn(min = size.height).scale(scale),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(cornerRadius),
        colors = style.colors(disabled = isVisuallyDisabled),
        elevation = null,
        border = style.border(disabled = isVisuallyDisabled),
        contentPadding = size.contentPadding,
        interactionSource = interactionSource,
    ) {
        if (isLoading) {
            LoadingIndicator(
                modifier =
                    Modifier.size(size.loadingIndicatorSize).semantics {
                        if (loadingContentDescription != null) {
                            contentDescription = loadingContentDescription
                        }
                    },
                color = LocalContentColor.current,
            )
        } else {
            Text(text, style = size.labelStyle, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RepzButtonStyle.colors(disabled: Boolean): ButtonColors {
    val disabledContainer = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)

    return when (this) {
        RepzButtonStyle.Filled -> {
            val container = MaterialTheme.colorScheme.primary
            val content = MaterialTheme.colorScheme.onPrimary
            ButtonColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = if (disabled) disabledContainer else container,
                disabledContentColor = if (disabled) disabledContent else content,
            )
        }

        RepzButtonStyle.Outlined, RepzButtonStyle.Text -> {
            val content = MaterialTheme.colorScheme.primary
            ButtonColors(
                containerColor = Color.Transparent,
                contentColor = content,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = if (disabled) disabledContent else content,
            )
        }
    }
}

@Composable
private fun RepzButtonStyle.border(disabled: Boolean): BorderStroke? =
    when (this) {
        RepzButtonStyle.Outlined -> {
            val color =
                if (disabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
                } else {
                    MaterialTheme.colorScheme.outline
                }
            BorderStroke(OutlinedButtonBorderWidth, color)
        }

        RepzButtonStyle.Filled, RepzButtonStyle.Text -> {
            null
        }
    }
