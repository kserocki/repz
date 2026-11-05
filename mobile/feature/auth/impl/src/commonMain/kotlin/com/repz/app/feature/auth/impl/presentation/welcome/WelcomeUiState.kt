package com.repz.app.feature.auth.impl.presentation.welcome

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** The Welcome pager's three pages, in display order (Q1: backdrop shape only, no live illustrations). */
enum class WelcomePage { LogSets, WorksOffline, Progress }

private val ALL_WELCOME_PAGES: ImmutableList<WelcomePage> = WelcomePage.entries.toImmutableList()

/** Welcome has nothing to load or decide, so it has no ViewModel (AGENTS.md §4 covers screens that need one). */
data class WelcomeUiState(
    val pages: ImmutableList<WelcomePage> = ALL_WELCOME_PAGES,
)
