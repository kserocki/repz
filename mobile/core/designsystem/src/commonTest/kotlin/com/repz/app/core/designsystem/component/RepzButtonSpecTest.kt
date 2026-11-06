package com.repz.app.core.designsystem.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class RepzButtonSpecTest {
    @Test
    fun given_everySize_whenReadingHeight_thenMatchesDesignSystem() {
        assertEquals(32.dp, RepzButtonSize.ExtraSmall.height)
        assertEquals(40.dp, RepzButtonSize.Small.height)
        assertEquals(56.dp, RepzButtonSize.Medium.height)
        assertEquals(96.dp, RepzButtonSize.Large.height)
        assertEquals(136.dp, RepzButtonSize.ExtraLarge.height)
    }

    @Test
    fun given_everySize_whenReadingRestCornerRadius_thenItIsHalfTheHeight() {
        for (size in RepzButtonSize.entries) {
            assertEquals(size.height / 2, size.restCornerRadius, "${size.name}.restCornerRadius")
        }
    }

    @Test
    fun given_everySizeAndStyle_whenPressed_thenMorphsToTheSharedPressedRadius() {
        // T13: every non-Hero-component size/style shares one pressed radius (12 dp), DESIGN_SYSTEM §5.
        assertEquals(12.dp, PressedCornerRadius)
    }

    @Test
    fun given_everySize_whenReadingHorizontalContentPadding_thenMatchesBoard04() {
        // T14: 12 / 16 / 24 / 36 / 48 dp horizontal, exact board values (not M3's own L/XL defaults).
        assertEquals(12.dp, RepzButtonSize.ExtraSmall.horizontalContentPadding)
        assertEquals(16.dp, RepzButtonSize.Small.horizontalContentPadding)
        assertEquals(24.dp, RepzButtonSize.Medium.horizontalContentPadding)
        assertEquals(36.dp, RepzButtonSize.Large.horizontalContentPadding)
        assertEquals(48.dp, RepzButtonSize.ExtraLarge.horizontalContentPadding)
    }

    @Test
    fun given_everySize_whenReadingLabelStyleToken_thenMatchesU6c() {
        assertEquals(RepzButtonLabelStyleToken.LabelMedium, RepzButtonSize.ExtraSmall.labelStyleToken)
        assertEquals(RepzButtonLabelStyleToken.LabelLarge, RepzButtonSize.Small.labelStyleToken)
        assertEquals(RepzButtonLabelStyleToken.TitleMedium, RepzButtonSize.Medium.labelStyleToken)
        assertEquals(RepzButtonLabelStyleToken.HeadlineSmall, RepzButtonSize.Large.labelStyleToken)
        assertEquals(RepzButtonLabelStyleToken.HeadlineLarge, RepzButtonSize.ExtraLarge.labelStyleToken)
    }

    @Test
    fun given_everyStyle_whenReadingColorRoles_thenMatchesDesignSystem() {
        assertEquals(
            RepzButtonColorRoles(RepzButtonColorRole.Primary, RepzButtonColorRole.OnPrimary),
            RepzButtonStyle.Filled.colorRoles,
        )
        assertEquals(
            RepzButtonColorRoles(RepzButtonColorRole.SecondaryContainer, RepzButtonColorRole.OnSecondaryContainer),
            RepzButtonStyle.Tonal.colorRoles,
        )
        assertEquals(
            RepzButtonColorRoles(RepzButtonColorRole.PrimaryContainer, RepzButtonColorRole.OnPrimaryContainer),
            RepzButtonStyle.Hero.colorRoles,
        )
        assertEquals(
            RepzButtonColorRoles(RepzButtonColorRole.Transparent, RepzButtonColorRole.Primary),
            RepzButtonStyle.Outlined.colorRoles,
        )
        assertEquals(
            RepzButtonColorRoles(RepzButtonColorRole.Transparent, RepzButtonColorRole.Primary),
            RepzButtonStyle.Text.colorRoles,
        )
    }
}
