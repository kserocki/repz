package com.repz.app.core.designsystem.logo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A static or externally animated symbol (the 7-part dumbbell, brand/LOGO.md §2), sized by its
 * artwork width ([RepzLogoGeometry.artworkAspectRatio], 2.3125 : 1): set a width with [modifier]
 * and the height follows. [state] defaults to the assembled rest pose; pass [RepzLaunchLogo]'s or
 * a shared [rememberRepzLogoSymbolState] to drive it externally.
 *
 * Reads its own position in the window and the window's width ([LocalWindowInfo]) so that an
 * animated [state] slides a plate fully past its true screen edge, not just this composable's own
 * bounds — needed only while [state] actually moves a plate away from rest (only [RepzLaunchLogo]
 * does).
 */
@Composable
fun RepzLogoSymbol(
    modifier: Modifier = Modifier,
    color: Color = RepzLogoDefaults.color(),
    state: RepzLogoSymbolState = rememberRepzLogoSymbolState(),
    contentDescription: String? = null,
) {
    var symbolLeftInWindowPx by remember { mutableFloatStateOf(0f) }
    val windowWidthPx =
        LocalWindowInfo.current.containerSize.width
            .toFloat()

    val semanticsModifier =
        if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
        } else {
            Modifier
        }

    RepzLogoSymbolCanvas(
        state = state,
        color = color,
        modifier = modifier.onGloballyPositioned { symbolLeftInWindowPx = it.positionInWindow().x }.then(semanticsModifier),
        plateDistancePx = { part, artworkWidthPx ->
            plateEntryDistancePx(part, symbolLeftInWindowPx, artworkWidthPx, windowWidthPx)
        },
    )
}

private val DefaultOutlineStrokeWidth = 1.5.dp

// Board 07 A5's pending-pair outline: a 6-grid-unit dash on a 3-grid-unit stroke, i.e. twice the
// stroke width.
private const val DASH_ON_OFF_RATIO = 2f

/**
 * The 7-part drawing shared by [RepzLogoSymbol] and [RepzLoader]: one [Canvas], [state]'s
 * animatables read directly inside the draw phase (redraw only, no recomposition, on every tick).
 * [plateDistancePx] is each caller's own notion of "fully past the edge": [RepzLogoSymbol] uses the
 * real screen edge, [RepzLoader] a fixed one-plate-width local distance (brand/LOGO.md §5, MOT-25).
 */
@Composable
internal fun RepzLogoSymbolCanvas(
    state: RepzLogoSymbolState,
    color: Color,
    plateDistancePx: (part: RepzLogoPart, artworkWidthPx: Float) -> Float,
    modifier: Modifier = Modifier,
    outlineStrokeWidth: Dp = DefaultOutlineStrokeWidth,
    outlineColor: Color = color,
) {
    val outlineStrokePx = with(LocalDensity.current) { outlineStrokeWidth.toPx() }

    Canvas(modifier = modifier.aspectRatio(RepzLogoGeometry.artworkAspectRatio)) {
        val artworkWidthPx = size.width
        scale(scaleX = 1f, scaleY = state.symbolScaleY.value, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawLogoPart(
                part = RepzLogoPart.Bar,
                artworkWidthPx = artworkWidthPx,
                color = color,
                outlineColor = outlineColor,
                fillAlpha = 1f,
                outlineAlpha = 0f,
                outlineStrokePx = outlineStrokePx,
            )

            for (pair in RepzPlatePair.entries) {
                val entry = state.plateEntry.getValue(pair).value
                val baseAlpha = state.plateAlpha.getValue(pair).value
                val outlined = state.isOutlined.getValue(pair).value

                for (part in pair.parts) {
                    val distance = plateDistancePx(part, artworkWidthPx)
                    translate(left = entry * distance) {
                        drawLogoPart(
                            part = part,
                            artworkWidthPx = artworkWidthPx,
                            color = color,
                            outlineColor = outlineColor,
                            fillAlpha = baseAlpha * (1f - outlined),
                            outlineAlpha = baseAlpha * outlined,
                            outlineStrokePx = outlineStrokePx,
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLogoPart(
    part: RepzLogoPart,
    artworkWidthPx: Float,
    color: Color,
    outlineColor: Color,
    fillAlpha: Float,
    outlineAlpha: Float,
    outlineStrokePx: Float,
) {
    val rect = partRectPx(part, artworkWidthPx)
    val cornerRadius = CornerRadius(minOf(rect.width, rect.height) / 2f)
    if (fillAlpha > 0f) {
        drawRoundRect(color = color, topLeft = rect.topLeft, size = rect.size, cornerRadius = cornerRadius, alpha = fillAlpha)
    }
    if (outlineAlpha > 0f) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(outlineStrokePx * DASH_ON_OFF_RATIO, outlineStrokePx * DASH_ON_OFF_RATIO))
        drawRoundRect(
            color = outlineColor,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = cornerRadius,
            alpha = outlineAlpha,
            style = Stroke(width = outlineStrokePx, pathEffect = dash),
        )
    }
}
