package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
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

enum class RepzButtonStyle { Filled, Tonal, Outlined, Text, Hero }

enum class RepzButtonSize { ExtraSmall, Small, Medium, Large, ExtraLarge }

// DESIGN_SYSTEM §9 "RepzButton": XS 32, S 40, M 56, L 96, XL 136 (matches M3 Expressive's own
// 5-size button scale exactly, so ButtonDefaults.iconSizeFor/iconSpacingFor resolve correctly below).
private val ExtraSmallHeight = 32.dp
private val SmallHeight = 40.dp
private val MediumHeight = 56.dp
private val LargeHeight = 96.dp
private val ExtraLargeHeight = 136.dp

// DESIGN_SYSTEM §5 "medium" (12 dp, "pressed buttons"): the shared pressed corner radius (MOT-22),
// every size and non-Hero-component style. RepzHeroStartButton (a different component) has its own
// 22 dp pressed radius.
internal val PressedCornerRadius = 12.dp

// DESIGN_SYSTEM §9 "disabled onSurface 12 % container / 38 % content" (board 04).
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

// MOT-22 press feedback (MOTION.md): scale to 0.96 on press.
private const val PRESSED_BUTTON_SCALE = 0.96f

private val OutlinedButtonBorderWidth = 1.dp

// Board 04 §1 exact horizontal content padding per size (T14); vertical keeps M3 Expressive's own
// padding for the matching tier, since the board only pins horizontal values.
private val ExtraSmallHorizontalPadding = 12.dp
private val SmallHorizontalPadding = 16.dp
private val MediumHorizontalPadding = 24.dp
private val LargeHorizontalPadding = 36.dp
private val ExtraLargeHorizontalPadding = 48.dp

internal val RepzButtonSize.height: Dp
    get() =
        when (this) {
            RepzButtonSize.ExtraSmall -> ExtraSmallHeight
            RepzButtonSize.Small -> SmallHeight
            RepzButtonSize.Medium -> MediumHeight
            RepzButtonSize.Large -> LargeHeight
            RepzButtonSize.ExtraLarge -> ExtraLargeHeight
        }

// A rest-state corner radius of half the height renders the same stadium (pill) shape as
// RepzShapes.full (CircleShape), but as a Dp so it can animate smoothly toward PressedCornerRadius.
internal val RepzButtonSize.restCornerRadius: Dp
    get() = height / 2

/** The M3 token this size's label resolves to (DESIGN_SYSTEM §9 U6c), kept as a plain enum so the
 * size → style mapping is testable without a composition. */
internal enum class RepzButtonLabelStyleToken { LabelMedium, LabelLarge, TitleMedium, HeadlineSmall, HeadlineLarge }

// Q12/U6c: XS/S/M keep their landed mapping; L/M3's own headlineSmall/headlineLarge breakpoints for
// 96/136 dp buttons happen to match DESIGN_SYSTEM's L/XL exactly (ButtonDefaults.labelStyleFor).
internal val RepzButtonSize.labelStyleToken: RepzButtonLabelStyleToken
    get() =
        when (this) {
            RepzButtonSize.ExtraSmall -> RepzButtonLabelStyleToken.LabelMedium
            RepzButtonSize.Small -> RepzButtonLabelStyleToken.LabelLarge
            RepzButtonSize.Medium -> RepzButtonLabelStyleToken.TitleMedium
            RepzButtonSize.Large -> RepzButtonLabelStyleToken.HeadlineSmall
            RepzButtonSize.ExtraLarge -> RepzButtonLabelStyleToken.HeadlineLarge
        }

@Composable
private fun RepzButtonLabelStyleToken.resolve(): TextStyle =
    when (this) {
        RepzButtonLabelStyleToken.LabelMedium -> MaterialTheme.typography.labelMedium
        RepzButtonLabelStyleToken.LabelLarge -> MaterialTheme.typography.labelLarge
        RepzButtonLabelStyleToken.TitleMedium -> MaterialTheme.typography.titleMedium
        RepzButtonLabelStyleToken.HeadlineSmall -> MaterialTheme.typography.headlineSmall
        RepzButtonLabelStyleToken.HeadlineLarge -> MaterialTheme.typography.headlineLarge
    }

private val RepzButtonSize.labelStyle: TextStyle
    @Composable get() = labelStyleToken.resolve()

internal val RepzButtonSize.horizontalContentPadding: Dp
    get() =
        when (this) {
            RepzButtonSize.ExtraSmall -> ExtraSmallHorizontalPadding
            RepzButtonSize.Small -> SmallHorizontalPadding
            RepzButtonSize.Medium -> MediumHorizontalPadding
            RepzButtonSize.Large -> LargeHorizontalPadding
            RepzButtonSize.ExtraLarge -> ExtraLargeHorizontalPadding
        }

private val RepzButtonSize.verticalContentPadding: Dp
    get() =
        when (this) {
            RepzButtonSize.ExtraSmall -> ButtonDefaults.ExtraSmallContentPadding.calculateTopPadding()
            RepzButtonSize.Small -> ButtonDefaults.SmallContentPadding.calculateTopPadding()
            RepzButtonSize.Medium -> ButtonDefaults.MediumContentPadding.calculateTopPadding()
            RepzButtonSize.Large -> ButtonDefaults.LargeContentPadding.calculateTopPadding()
            RepzButtonSize.ExtraLarge -> ButtonDefaults.ExtraLargeContentPadding.calculateTopPadding()
        }

private val RepzButtonSize.contentPadding: PaddingValues
    get() = PaddingValues(horizontal = horizontalContentPadding, vertical = verticalContentPadding)

/** The role each [RepzButtonStyle] resolves its container/content colour from, kept as a plain enum
 * so the style → role mapping is testable without a composition. */
internal enum class RepzButtonColorRole {
    Primary,
    OnPrimary,
    SecondaryContainer,
    OnSecondaryContainer,
    PrimaryContainer,
    OnPrimaryContainer,
    Transparent,
}

internal data class RepzButtonColorRoles(
    val container: RepzButtonColorRole,
    val content: RepzButtonColorRole,
)

internal val RepzButtonStyle.colorRoles: RepzButtonColorRoles
    get() =
        when (this) {
            RepzButtonStyle.Filled -> {
                RepzButtonColorRoles(RepzButtonColorRole.Primary, RepzButtonColorRole.OnPrimary)
            }

            RepzButtonStyle.Tonal -> {
                RepzButtonColorRoles(RepzButtonColorRole.SecondaryContainer, RepzButtonColorRole.OnSecondaryContainer)
            }

            RepzButtonStyle.Hero -> {
                RepzButtonColorRoles(RepzButtonColorRole.PrimaryContainer, RepzButtonColorRole.OnPrimaryContainer)
            }

            RepzButtonStyle.Outlined, RepzButtonStyle.Text -> {
                RepzButtonColorRoles(RepzButtonColorRole.Transparent, RepzButtonColorRole.Primary)
            }
        }

@Composable
private fun RepzButtonColorRole.resolve(): Color =
    when (this) {
        RepzButtonColorRole.Primary -> MaterialTheme.colorScheme.primary
        RepzButtonColorRole.OnPrimary -> MaterialTheme.colorScheme.onPrimary
        RepzButtonColorRole.SecondaryContainer -> MaterialTheme.colorScheme.secondaryContainer
        RepzButtonColorRole.OnSecondaryContainer -> MaterialTheme.colorScheme.onSecondaryContainer
        RepzButtonColorRole.PrimaryContainer -> MaterialTheme.colorScheme.primaryContainer
        RepzButtonColorRole.OnPrimaryContainer -> MaterialTheme.colorScheme.onPrimaryContainer
        RepzButtonColorRole.Transparent -> Color.Transparent
    }

/**
 * The primary action control (DESIGN_SYSTEM §9, board 04): [RepzButtonStyle.Filled] (high
 * emphasis, `primary`/`onPrimary`; the decided CTA colour for every call to action, recorded in
 * DESIGN_SYSTEM §13), [RepzButtonStyle.Tonal] (medium-high emphasis, `secondaryContainer`/
 * `onSecondaryContainer`; e.g. "Save template"), [RepzButtonStyle.Outlined] (medium emphasis,
 * `outline` border), [RepzButtonStyle.Text] (low emphasis) or [RepzButtonStyle.Hero]
 * (`primaryContainer`/`onPrimaryContainer`, the brand-cyan treatment board 07 draws for its boldest
 * CTA; Welcome's "Create account" only, DESIGN_SYSTEM §13). [RepzButtonSize] ([RepzButtonSize.ExtraSmall]
 * 32 dp … [RepzButtonSize.ExtraLarge] 136 dp) is a minimum height: at larger font scales [text] wraps
 * to more lines and the button grows to fit, never clipping or truncating. [leadingIcon] is purely
 * decorative (the label already carries the meaning) and sized/spaced per
 * [ButtonDefaults.iconSizeFor]/[ButtonDefaults.iconSpacingFor] for [size]'s height.
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
    leadingIcon: RepzIcon? = null,
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
        RepzButtonBody(text, size, isLoading, loadingContentDescription, leadingIcon)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RepzButtonBody(
    text: String,
    size: RepzButtonSize,
    isLoading: Boolean,
    loadingContentDescription: String?,
    leadingIcon: RepzIcon?,
) {
    when {
        isLoading -> {
            LoadingIndicator(
                modifier =
                    Modifier.size(ButtonDefaults.iconSizeFor(size.height)).semantics {
                        if (loadingContentDescription != null) {
                            contentDescription = loadingContentDescription
                        }
                    },
                color = LocalContentColor.current,
            )
        }

        leadingIcon != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RepzIcon(
                    icon = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(size.height)),
                )
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(size.height)))
                Text(text, style = size.labelStyle, textAlign = TextAlign.Center)
            }
        }

        else -> {
            Text(text, style = size.labelStyle, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RepzButtonStyle.colors(disabled: Boolean): ButtonColors {
    val roles = colorRoles
    val container = roles.container.resolve()
    val content = roles.content.resolve()
    val disabledContainer = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    val isTransparentContainer = roles.container == RepzButtonColorRole.Transparent

    return ButtonColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = if (disabled && !isTransparentContainer) disabledContainer else container,
        disabledContentColor = if (disabled) disabledContent else content,
    )
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

        RepzButtonStyle.Filled, RepzButtonStyle.Text, RepzButtonStyle.Tonal, RepzButtonStyle.Hero -> {
            null
        }
    }
