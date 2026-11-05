package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.repz.app.core.designsystem.logo.RepzLoader
import com.repz.app.core.designsystem.logo.RepzLoaderSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val DETERMINATE_STEPS = listOf(0f, 0.34f, 0.67f, 1.0f)
private const val STEPPING_SPECIMEN_INTERVAL_MILLIS = 1500L
private const val STEPPING_SPECIMEN_STEP_COUNT = 4 // 3 thirds plus the reset back to 0.

/** `logo-loader` (MOT-25): indeterminate at all 3 sizes, 4 fixed determinate specimens (one pair
 * locked per third), and one specimen stepping 0 → 1 in thirds on a loop. */
@Composable
internal fun LogoLoaderCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        CatalogSectionLabel("indeterminate")
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            for (size in RepzLoaderSize.entries) {
                RepzLoader(contentDescription = CatalogSpecimens.LOADER_DESCRIPTION, size = size)
            }
        }

        CatalogSectionLabel("determinate — 0, ⅓, ⅔, 1")
        Row(horizontalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
            for (progress in DETERMINATE_STEPS) {
                RepzLoader(contentDescription = CatalogSpecimens.LOADER_DESCRIPTION, progress = progress)
            }
        }

        CatalogSectionLabel("determinate — stepping every 1.5 s")
        SteppingLoader()
    }
}

@Composable
private fun SteppingLoader() {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(STEPPING_SPECIMEN_INTERVAL_MILLIS)
            step = (step + 1) % STEPPING_SPECIMEN_STEP_COUNT
        }
    }
    RepzLoader(contentDescription = CatalogSpecimens.LOADER_DESCRIPTION, progress = step / (STEPPING_SPECIMEN_STEP_COUNT - 1).toFloat())
}
