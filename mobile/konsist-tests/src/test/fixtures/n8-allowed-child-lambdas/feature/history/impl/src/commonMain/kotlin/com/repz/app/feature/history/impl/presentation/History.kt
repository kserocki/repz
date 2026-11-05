package com.repz.app.feature.history.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

sealed interface HistoryIntent {
    data object Refresh : HistoryIntent
}

@Composable
fun HistoryScreen() {
}

@Composable
fun HistoryContent(
    state: Any,
    onIntent: (HistoryIntent) -> Unit,
) {
    HistoryRow(
        title = "Today",
        onRefresh = { onIntent(HistoryIntent.Refresh) },
    )
}

@Composable
fun HistoryRow(
    title: String,
    onRefresh: () -> Unit,
) {
}

@Preview
@Composable
fun HistoryContentPreview() {
    HistoryContent(state = Unit, onIntent = {})
}
