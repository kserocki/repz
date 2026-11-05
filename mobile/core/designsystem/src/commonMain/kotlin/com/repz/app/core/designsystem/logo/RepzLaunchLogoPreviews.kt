package com.repz.app.core.designsystem.logo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.logo_wordmark
import com.repz.app.core.designsystem.theme.RepzTheme
import org.jetbrains.compose.resources.painterResource

/** The finished MOT-01 frame (symbol assembled, wordmark fully visible), built directly rather than
 * by letting [RepzLaunchLogo] actually play the ~1.2 s choreography: a preview snapshot needs a
 * deterministic frame, not whichever one lands when the screenshot is taken. */
@Composable
private fun FinishedLaunchLogo(symbolWidth: Dp) {
    val layout = launchLogoLayout(symbolWidth)
    val color = RepzLogoDefaults.color()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RepzLogoSymbol(
            modifier = Modifier.width(symbolWidth),
            color = color,
            state = rememberRepzLogoSymbolState(RepzLogoPose.Assembled),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(layout.gapBelowPlates))
        Image(
            painter = painterResource(Res.drawable.logo_wordmark),
            contentDescription = null,
            modifier = Modifier.width(layout.wordmarkWidth).aspectRatio(layout.wordmarkAspectRatio),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(color),
        )
    }
}

@Composable
private fun RepzLaunchLogoStaticSymbolShowcase() {
    RepzLaunchLogo(playback = RepzLaunchLogoPlayback.StaticSymbol, onFinish = {})
}

@Composable
private fun RepzLaunchLogoFinishedShowcase() {
    FinishedLaunchLogo(symbolWidth = RepzLogoDefaults.SplashSymbolWidth)
}

@Preview
@Composable
private fun RepzLaunchLogoStaticSymbolLightPreview() {
    RepzTheme(darkTheme = false) { RepzLaunchLogoStaticSymbolShowcase() }
}

@Preview
@Composable
private fun RepzLaunchLogoStaticSymbolDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLaunchLogoStaticSymbolShowcase() }
}

@Preview
@Composable
private fun RepzLaunchLogoFinishedLightPreview() {
    RepzTheme(darkTheme = false) { RepzLaunchLogoFinishedShowcase() }
}

@Preview
@Composable
private fun RepzLaunchLogoFinishedDarkPreview() {
    RepzTheme(darkTheme = true) { RepzLaunchLogoFinishedShowcase() }
}
