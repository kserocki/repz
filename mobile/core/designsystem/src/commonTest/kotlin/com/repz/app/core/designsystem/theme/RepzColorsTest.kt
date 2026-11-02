package com.repz.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.repz.app.core.designsystem.RepzBrandColors
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzColorsTest {
    @Test
    fun given_lightColors_whenReadingBrandFields_thenReferenceRepzBrandColors() {
        assertEquals(RepzBrandColors.Cyan, RepzLightColors.brandCyan)
        assertEquals(RepzBrandColors.Deep, RepzLightColors.brandDeep)
        assertEquals(RepzBrandColors.Ink, RepzLightColors.brandInk)
        assertEquals(RepzBrandColors.Paper, RepzLightColors.brandPaper)
        assertEquals(RepzBrandColors.Mist, RepzLightColors.brandMist)
    }

    @Test
    fun given_darkColors_whenReadingBrandFields_thenReferenceRepzBrandColors() {
        assertEquals(RepzBrandColors.Cyan, RepzDarkColors.brandCyan)
        assertEquals(RepzBrandColors.Deep, RepzDarkColors.brandDeep)
        assertEquals(RepzBrandColors.Ink, RepzDarkColors.brandInk)
        assertEquals(RepzBrandColors.Paper, RepzDarkColors.brandPaper)
        assertEquals(RepzBrandColors.Mist, RepzDarkColors.brandMist)
    }

    @Test
    fun given_lightAndDarkColors_whenReadingBrandAndRecordBright_thenIdenticalInBothThemes() {
        assertEquals(RepzLightColors.brandCyan, RepzDarkColors.brandCyan)
        assertEquals(RepzLightColors.brandDeep, RepzDarkColors.brandDeep)
        assertEquals(RepzLightColors.brandInk, RepzDarkColors.brandInk)
        assertEquals(RepzLightColors.brandPaper, RepzDarkColors.brandPaper)
        assertEquals(RepzLightColors.brandMist, RepzDarkColors.brandMist)
        assertEquals(RepzLightColors.recordBright, RepzDarkColors.recordBright)
        assertEquals(Color(0xFFFCBC00), RepzLightColors.recordBright)
    }

    @Test
    fun given_lightColors_whenReadingEveryToken_thenMatchesDesignSystem() {
        with(RepzLightColors) {
            assertEquals(Color(0xFF296B2A), success)
            assertEquals(Color(0xFFFFFFFF), onSuccess)
            assertEquals(Color(0xFFACF4A2), successContainer)
            assertEquals(Color(0xFF002203), onSuccessContainer)
            assertEquals(Color(0xFF865300), warning)
            assertEquals(Color(0xFFFFFFFF), onWarning)
            assertEquals(Color(0xFFFFDDB8), warningContainer)
            assertEquals(Color(0xFF2B1700), onWarningContainer)
            assertEquals(Color(0xFF896500), record)
            assertEquals(Color(0xFFFFFFFF), onRecord)
            assertEquals(Color(0xFFFFDEA0), recordContainer)
            assertEquals(Color(0xFF261900), onRecordContainer)
            assertEquals(Color(0xFFFCBC00), recordBright)
            assertEquals(Color(0xFF4359A9), setWarmup)
            assertEquals(Color(0xFFFFFFFF), onSetWarmup)
            assertEquals(Color(0xFFDCE1FF), setWarmupContainer)
            assertEquals(Color(0xFF001551), onSetWarmupContainer)
            assertEquals(Color(0xFF595F61), setNormal)
            assertEquals(Color(0xFFFFFFFF), onSetNormal)
            assertEquals(Color(0xFFDDE3E6), setNormalContainer)
            assertEquals(Color(0xFF161D1E), onSetNormalContainer)
            assertEquals(Color(0xFF8D3F92), setDrop)
            assertEquals(Color(0xFFFFFFFF), onSetDrop)
            assertEquals(Color(0xFFFFD6FB), setDropContainer)
            assertEquals(Color(0xFF36003D), onSetDropContainer)
            assertEquals(Color(0xFFAF2E27), setFailure)
            assertEquals(Color(0xFFFFFFFF), onSetFailure)
            assertEquals(Color(0xFFFFDAD6), setFailureContainer)
            assertEquals(Color(0xFF410002), onSetFailureContainer)
            assertEquals(Color(0xFFF04600), musclePrimary)
            assertEquals(Color(0xFFFFB779), muscleSecondary)
            assertEquals(Color(0xFFDEE3E5), muscleInactive)
            assertEquals(Color(0xFF006879), chartSeries1)
            assertEquals(Color(0xFFB34B00), chartSeries2)
            assertEquals(Color(0xFF6F44FC), chartSeries3)
            assertEquals(Color(0xFFDEE3E5), chartGrid)
            assertEquals(Color(0xFF666C6D), chartAxis)
        }
    }

    @Test
    fun given_darkColors_whenReadingEveryToken_thenMatchesDesignSystem() {
        with(RepzDarkColors) {
            assertEquals(Color(0xFF91D888), success)
            assertEquals(Color(0xFF003908), onSuccess)
            assertEquals(Color(0xFF0B5314), successContainer)
            assertEquals(Color(0xFFACF4A2), onSuccessContainer)
            assertEquals(Color(0xFFFFB960), warning)
            assertEquals(Color(0xFF472A00), onWarning)
            assertEquals(Color(0xFF653E00), warningContainer)
            assertEquals(Color(0xFFFFDDB8), onWarningContainer)
            assertEquals(Color(0xFFFCBC00), record)
            assertEquals(Color(0xFF261900), onRecord)
            assertEquals(Color(0xFF5C4300), recordContainer)
            assertEquals(Color(0xFFFFDEA0), onRecordContainer)
            assertEquals(Color(0xFFFCBC00), recordBright)
            assertEquals(Color(0xFFB7C4FF), setWarmup)
            assertEquals(Color(0xFF0B2878), onSetWarmup)
            assertEquals(Color(0xFF294190), setWarmupContainer)
            assertEquals(Color(0xFFDCE1FF), onSetWarmupContainer)
            assertEquals(Color(0xFFBBC9CD), setNormal)
            assertEquals(Color(0xFF2B3133), onSetNormal)
            assertEquals(Color(0xFF2F3638), setNormalContainer)
            assertEquals(Color(0xFFDDE3E6), onSetNormalContainer)
            assertEquals(Color(0xFFFFA9FF), setDrop)
            assertEquals(Color(0xFF57065F), onSetDrop)
            assertEquals(Color(0xFF712578), setDropContainer)
            assertEquals(Color(0xFFFFD6FB), onSetDropContainer)
            assertEquals(Color(0xFFFFB4AB), setFailure)
            assertEquals(Color(0xFF690005), onSetFailure)
            assertEquals(Color(0xFF8E1313), setFailureContainer)
            assertEquals(Color(0xFFFFDAD6), onSetFailureContainer)
            assertEquals(Color(0xFFFF7348), musclePrimary)
            assertEquals(Color(0xFFC56E00), muscleSecondary)
            assertEquals(Color(0xFF404749), muscleInactive)
            assertEquals(Color(0xFF46DEFD), chartSeries1)
            assertEquals(Color(0xFFFFA273), chartSeries2)
            assertEquals(Color(0xFFBDAEFF), chartSeries3)
            assertEquals(Color(0xFF363C3E), chartGrid)
            assertEquals(Color(0xFFA6ACAE), chartAxis)
        }
    }
}
