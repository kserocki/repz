package com.repz.app.feature.history.impl.presentation

import androidx.compose.runtime.Composable

sealed interface HistoryIntent {
    data object Refresh : HistoryIntent
}

@Composable
fun HistoryRow(onIntent: (HistoryIntent) -> Unit) {
}
