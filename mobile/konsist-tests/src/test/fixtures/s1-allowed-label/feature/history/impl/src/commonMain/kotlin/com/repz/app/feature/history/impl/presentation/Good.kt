package com.repz.app.feature.history.impl.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable

@Composable
fun Good() {
    val alpha by animateFloatAsState(targetValue = 1f, label = "fadeAlpha")
}
