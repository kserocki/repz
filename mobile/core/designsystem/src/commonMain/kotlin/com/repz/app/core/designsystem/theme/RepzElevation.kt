package com.repz.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** One elevation level (DESIGN_SYSTEM §7): a tonal container colour, plus a shadow for the levels
 * that float under the finger. Components draw the shadow with `Modifier.dropShadow(shape,
 * shadow)`. */
@Immutable
class RepzElevationLevel(
    val containerColor: Color,
    val shadow: Shadow?,
)

@Immutable
class RepzElevationScheme(
    val level0: RepzElevationLevel,
    val level1: RepzElevationLevel,
    val level2: RepzElevationLevel,
    val level3: RepzElevationLevel,
    val level4: RepzElevationLevel,
    val fab: RepzElevationLevel,
)

private val Level4Shadow = Shadow(radius = 20.dp, color = Color(0x66000000), offset = DpOffset(0.dp, 8.dp))

// Board 18's fab shadow colour (DESIGN_SYSTEM §7): a purple tint, light theme only.
private val FabShadowLight = Shadow(radius = 12.dp, color = Color(0x3828145A), offset = DpOffset(0.dp, 4.dp))

internal fun repzElevationScheme(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
): RepzElevationScheme =
    RepzElevationScheme(
        level0 = RepzElevationLevel(containerColor = colorScheme.surface, shadow = null),
        level1 = RepzElevationLevel(containerColor = colorScheme.surfaceContainerLow, shadow = null),
        level2 = RepzElevationLevel(containerColor = colorScheme.surfaceContainer, shadow = null),
        level3 = RepzElevationLevel(containerColor = colorScheme.surfaceContainerHigh, shadow = null),
        level4 = RepzElevationLevel(containerColor = colorScheme.surfaceContainerHighest, shadow = Level4Shadow),
        fab =
            RepzElevationLevel(
                containerColor = colorScheme.primaryContainer,
                shadow = if (darkTheme) null else FabShadowLight,
            ),
    )

internal val LocalRepzElevation =
    staticCompositionLocalOf<RepzElevationScheme> {
        error("RepzElevationScheme not provided: wrap the content in RepzTheme.")
    }

/** `RepzTheme`'s elevation accessor: `RepzElevation.level3`, `RepzElevation.fab`, … */
object RepzElevation {
    val level0: RepzElevationLevel
        @Composable @ReadOnlyComposable
        get() = LocalRepzElevation.current.level0

    val level1: RepzElevationLevel
        @Composable @ReadOnlyComposable
        get() = LocalRepzElevation.current.level1

    val level2: RepzElevationLevel
        @Composable @ReadOnlyComposable
        get() = LocalRepzElevation.current.level2

    val level3: RepzElevationLevel
        @Composable @ReadOnlyComposable
        get() = LocalRepzElevation.current.level3

    val level4: RepzElevationLevel
        @Composable @ReadOnlyComposable
        get() = LocalRepzElevation.current.level4

    val fab: RepzElevationLevel
        @Composable @ReadOnlyComposable
        get() = LocalRepzElevation.current.fab
}
