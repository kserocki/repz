package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_edit
import com.repz.app.core.designsystem.generated.resources.symbol_keyboard_arrow_down
import com.repz.app.core.designsystem.generated.resources.symbol_more_vert
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource

// DESIGN_SYSTEM §6 "Live-workout header: 92"; a minimum, not a fixed, height, since a 2-line
// wrapped name or the fontScale ≥ 1.5 full-width Finish row both need to grow it.
private val HeaderMinHeight = 92.dp

// Board 14: at fontScale ≥ 1.5, Finish moves off the title row onto its own full-width row below.
private const val WIDE_FINISH_ROW_FONT_SCALE_THRESHOLD = 1.5f

/**
 * The live workout's header (DESIGN_SYSTEM §9 `LiveWorkoutHeader`, board 09/14): `surfaceContainerLow`
 * (level1) container, minimize chevron, an inline-editable [name] with elapsed time below it, the
 * [finishLabel] action and an overflow menu ([overflowItems], e.g. "Discard workout"). [elapsedText]
 * renders in `RepzTheme.typography.numericLarge` and is never announced per-tick (DESIGN_SYSTEM
 * §12): the caller is responsible for not wrapping it in a live region.
 */
@Composable
fun RepzLiveWorkoutHeader(
    name: String,
    elapsedText: String,
    isFinishEnabled: Boolean,
    finishLabel: String,
    isEditingName: Boolean,
    onNameClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameEditingFinish: () -> Unit,
    onMinimizeClick: () -> Unit,
    onFinishClick: () -> Unit,
    overflowItems: ImmutableList<RepzMenuItem>,
    minimizeContentDescription: String,
    editNameContentDescription: String,
    moreOptionsContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val isWideFinishRow = LocalDensity.current.fontScale >= WIDE_FINISH_ROW_FONT_SCALE_THRESHOLD

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = HeaderMinHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = RepzSpacing.lg, vertical = RepzSpacing.sm),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMinimizeClick) {
                Icon(
                    painter = painterResource(Res.drawable.symbol_keyboard_arrow_down),
                    contentDescription = minimizeContentDescription,
                )
            }
            RepzLiveWorkoutHeaderTitle(
                name = name,
                elapsedText = elapsedText,
                isEditingName = isEditingName,
                onNameClick = onNameClick,
                onNameChange = onNameChange,
                onNameEditingFinish = onNameEditingFinish,
                editNameContentDescription = editNameContentDescription,
                modifier = Modifier.weight(1f),
            )
            if (!isWideFinishRow) {
                RepzLiveWorkoutFinishButton(finishLabel, isFinishEnabled, onFinishClick)
            }
            RepzLiveWorkoutHeaderOverflow(items = overflowItems, contentDescription = moreOptionsContentDescription)
        }
        if (isWideFinishRow) {
            RepzLiveWorkoutFinishButton(
                finishLabel,
                isFinishEnabled,
                onFinishClick,
                modifier = Modifier.fillMaxWidth().padding(top = RepzSpacing.sm),
            )
        }
    }
}

@Composable
private fun RepzLiveWorkoutFinishButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RepzButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        style = RepzButtonStyle.Filled,
        size = RepzButtonSize.Small,
        enabled = enabled,
    )
}

@Composable
private fun RepzLiveWorkoutHeaderTitle(
    name: String,
    elapsedText: String,
    isEditingName: Boolean,
    onNameClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onNameEditingFinish: () -> Unit,
    editNameContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (isEditingName) {
            RepzLiveWorkoutNameField(name, onNameChange, onNameEditingFinish)
        } else {
            val nameRowDescription = nameRowContentDescription(name, editNameContentDescription)
            Row(
                modifier =
                    Modifier
                        .clickable(onClick = onNameClick)
                        .semantics(mergeDescendants = true) { contentDescription = nameRowDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs),
            ) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    painter = painterResource(Res.drawable.symbol_edit),
                    contentDescription = null,
                    modifier = Modifier.size(RepzIconSize.dense),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(elapsedText, style = RepzTheme.typography.numericLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// Built outside the `semantics { contentDescription = … }` block so a Konsist heuristic reading
// for hard-coded user-facing text doesn't mistake this accessibility-label composition (two already
// localized strings) for one.
private fun nameRowContentDescription(
    name: String,
    editNameContentDescription: String,
): String = "$name. $editNameContentDescription"

@Composable
private fun RepzLiveWorkoutNameField(
    name: String,
    onNameChange: (String) -> Unit,
    onNameEditingFinish: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = name,
        onValueChange = onNameChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState -> if (!focusState.isFocused) onNameEditingFinish() },
        textStyle = MaterialTheme.typography.titleLarge.copy(color = textColor),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onNameEditingFinish() }),
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun RepzLiveWorkoutHeaderOverflow(
    items: ImmutableList<RepzMenuItem>,
    contentDescription: String,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(painter = painterResource(Res.drawable.symbol_more_vert), contentDescription = contentDescription)
        }
        RepzWorkoutDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }, items = items)
    }
}
