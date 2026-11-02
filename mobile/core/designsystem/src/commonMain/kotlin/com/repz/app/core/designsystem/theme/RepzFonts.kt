package com.repz.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.archivo_variable
import com.repz.app.core.designsystem.generated.resources.inter_variable
import org.jetbrains.compose.resources.Font as RepzResourceFont

/** The two variable font families bundled as Compose resources (DESIGN_SYSTEM §3.3/§4). */
internal enum class RepzFontFile {
    Archivo,
    Inter,
}

/**
 * A single point on a variable font's axes.
 *
 * [width] only applies to Archivo (`wdth` 62–125); Inter has no `wdth` axis. [opticalSize] only
 * applies to Inter (`opsz` 14–32); Archivo has no `opsz` axis. Width is not a [FontFamily] matching
 * key the way weight or italic are, so every distinct spec needs its own [FontFamily].
 */
internal data class RepzFontSpec(
    val file: RepzFontFile,
    val weight: Int,
    val width: Float = 100f,
    val opticalSize: Float? = null,
)

/** Resolves a [RepzFontSpec] to the [FontFamily] built for it. */
internal fun interface RepzFontFamilies {
    fun familyFor(spec: RepzFontSpec): FontFamily
}

/** Loads one [FontFamily] per distinct [RepzFontSpec] used by [RepzTypeScale], memoized for the
 * composition. */
@Composable
internal fun rememberRepzFontFamilies(): RepzFontFamilies {
    val fontsBySpec = RepzTypeScale.distinctFontSpecs.associateWith { spec -> resolveFont(spec) }
    return remember(fontsBySpec) {
        RepzFontFamilies { spec -> FontFamily(fontsBySpec.getValue(spec)) }
    }
}

@Composable
private fun resolveFont(spec: RepzFontSpec) =
    RepzResourceFont(
        resource = fontResourceFor(spec.file),
        weight = FontWeight(spec.weight),
        variationSettings = FontVariation.Settings(*variationSettingsFor(spec).toTypedArray()),
    )

private fun fontResourceFor(file: RepzFontFile) =
    when (file) {
        RepzFontFile.Archivo -> Res.font.archivo_variable
        RepzFontFile.Inter -> Res.font.inter_variable
    }

private fun variationSettingsFor(spec: RepzFontSpec): List<FontVariation.Setting> =
    buildList {
        add(FontVariation.weight(spec.weight))
        if (spec.file == RepzFontFile.Archivo) add(FontVariation.width(spec.width))
        spec.opticalSize?.let { add(FontVariation.Setting("opsz", it)) }
    }
