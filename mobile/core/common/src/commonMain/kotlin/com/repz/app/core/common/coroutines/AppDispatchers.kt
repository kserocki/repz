package com.repz.app.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatchers logic is injected with, instead of reading `Dispatchers.Default`/`Dispatchers.IO`
 * directly. A data class so a test builds one with a single test dispatcher for both fields, with
 * no fake class needed. Main is left out: ViewModels already run on `viewModelScope`'s Main.
 */
data class AppDispatchers(
    val default: CoroutineDispatcher,
    val io: CoroutineDispatcher,
)
