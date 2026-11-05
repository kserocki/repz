package com.repz.app.feature.auth.impl.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzPageIndicator
import com.repz.app.core.designsystem.theme.RepzShapes
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.feature.auth.impl.generated.resources.Res
import com.repz.app.feature.auth.impl.generated.resources.auth_create_account
import com.repz.app.feature.auth.impl.generated.resources.auth_log_in
import com.repz.app.feature.auth.impl.generated.resources.welcome_page_indicator
import com.repz.app.feature.auth.impl.generated.resources.welcome_page_log_sets_body
import com.repz.app.feature.auth.impl.generated.resources.welcome_page_log_sets_title
import com.repz.app.feature.auth.impl.generated.resources.welcome_page_progress_body
import com.repz.app.feature.auth.impl.generated.resources.welcome_page_progress_title
import com.repz.app.feature.auth.impl.generated.resources.welcome_page_works_offline_body
import com.repz.app.feature.auth.impl.generated.resources.welcome_page_works_offline_title
import com.repz.app.feature.auth.impl.generated.resources.welcome_skip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// Q1: the page-1 backdrop shape on every page, no live illustration content. Shape/colour per
// Muse's board-07 measurement vs. DESIGN_SYSTEM §5 check (2026-11-05): using the documented
// "illustration backdrop" accent (cookie4Sided) and the plan's surfaceContainerHigh; flagged as a
// question (board 07's path measures as a 9-lobed cookie on a different container colour) for
// confirmation.
private const val BACKDROP_WIDTH_FRACTION = 0.8f

// Capped by height as well as width: on a taller phone, width is always the binding constraint and
// this never applies. On a shorter/more square viewport, sizing the 1:1 backdrop from width alone
// left no room for the title and body underneath (iOS QA bug: body silently clipped, PL title cut
// mid-word). Never let the decorative backdrop claim more than this share of the page's height.
private const val BACKDROP_MAX_HEIGHT_FRACTION = 0.4f

/**
 * The 3-page pager (DESIGN_SYSTEM §9/§10 A2, board 07): Skip (text button, hidden on the last
 * page, Q9) scrolls to the last page instead of sending a [WelcomeIntent]; Create account and Log
 * in send [WelcomeIntent.CreateAccountClicked]/[WelcomeIntent.LogInClicked]. No MOT entry governs
 * the pager itself yet (Q2), so paging and the Skip scroll are both un-animated beyond
 * [HorizontalPager]'s own default swipe/`animateScrollToPage`.
 */
@Composable
fun WelcomeContent(
    state: WelcomeUiState,
    pagerState: PagerState,
    onIntent: (WelcomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == state.pages.lastIndex

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (!isLastPage) {
                RepzButton(
                    stringResource(Res.string.welcome_skip),
                    onClick = { scope.launch { pagerState.animateScrollToPage(state.pages.lastIndex) } },
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(horizontal = RepzSpacing.xl, vertical = RepzSpacing.md)
                            .testTag("welcome_skip"),
                    style = RepzButtonStyle.Text,
                    size = RepzButtonSize.Small,
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            WelcomePageBody(state.pages[page], modifier = Modifier.fillMaxSize().padding(horizontal = RepzSpacing.xl))
        }

        RepzPageIndicator(
            pageCount = state.pages.size,
            currentPage = pagerState.currentPage,
            contentDescription = stringResource(Res.string.welcome_page_indicator, pagerState.currentPage + 1, state.pages.size),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = RepzSpacing.lg),
        )

        Column(
            modifier =
                Modifier
                    .padding(horizontal = RepzSpacing.xl)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = RepzSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
        ) {
            RepzButton(
                stringResource(Res.string.auth_create_account),
                onClick = { onIntent(WelcomeIntent.CreateAccountClicked) },
                modifier = Modifier.fillMaxWidth().testTag("welcome_create_account"),
                style = RepzButtonStyle.Filled,
                size = RepzButtonSize.Medium,
            )
            RepzButton(
                stringResource(Res.string.auth_log_in),
                onClick = { onIntent(WelcomeIntent.LogInClicked) },
                modifier = Modifier.fillMaxWidth().testTag("welcome_log_in"),
                style = RepzButtonStyle.Outlined,
                size = RepzButtonSize.Medium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomePageBody(
    page: WelcomePage,
    modifier: Modifier = Modifier,
) {
    val (titleRes, bodyRes) =
        when (page) {
            WelcomePage.LogSets -> Res.string.welcome_page_log_sets_title to Res.string.welcome_page_log_sets_body
            WelcomePage.WorksOffline -> Res.string.welcome_page_works_offline_title to Res.string.welcome_page_works_offline_body
            WelcomePage.Progress -> Res.string.welcome_page_progress_title to Res.string.welcome_page_progress_body
        }

    BoxWithConstraints(modifier = modifier) {
        val backdropSize = minOf(maxWidth * BACKDROP_WIDTH_FRACTION, maxHeight * BACKDROP_MAX_HEIGHT_FRACTION)

        // Scrollable, not just centered: a safety net so the body stays reachable instead of being
        // silently clipped (the same approach Register/Login already use for their forms) even past
        // the backdrop's own height cap above, e.g. at a very large font scale. A no-op on a normal
        // phone, where the content already fits and verticalArrangement centers it as before.
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(backdropSize)
                        .background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RepzShapes.cookie4Sided.toShape())
                        .clearAndSetSemantics {},
            )
            Text(
                stringResource(titleRes),
                modifier = Modifier.padding(top = RepzSpacing.xl),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLargeEmphasized,
            )
            Text(
                stringResource(bodyRes),
                modifier = Modifier.padding(top = RepzSpacing.sm),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
