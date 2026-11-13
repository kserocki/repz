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
 * The live workout's header (DESIGN_SYSTEM §9 `LiveWorkoutHeader`, board 09/14): minimize chevron,
 * an inline-editable [name] with elapsed time below it, [finishLabel] and an overflow menu.
 * [elapsedText] is never announced per-tick (DESIGN_SYSTEM §12) — the caller must not wrap it in a
 * live region. [minimizeButtonModifier]/[nameModifier]/[finishButtonModifier]/
 * [overflowButtonModifier] tag those elements individually since [modifier] lands on the header.
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
    nameContentDescription: String,
    moreOptionsContentDescription: String,
    modifier: Modifier = Modifier,
    minimizeButtonModifier: Modifier = Modifier,
    nameModifier: Modifier = Modifier,
    finishButtonModifier: Modifier = Modifier,
    overflowButtonModifier: Modifier = Modifier,
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
            IconButton(onClick = onMinimizeClick, modifier = minimizeButtonModifier) {
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
                nameContentDescription = nameContentDescription,
                modifier = Modifier.weight(1f),
                nameModifier = nameModifier,
            )
            if (!isWideFinishRow) {
                RepzLiveWorkoutFinishButton(finishLabel, isFinishEnabled, onFinishClick, modifier = finishButtonModifier)
            }
            RepzLiveWorkoutHeaderOverflow(
                items = overflowItems,
                contentDescription = moreOptionsContentDescription,
                buttonModifier = overflowButtonModifier,
            )
        }
        if (isWideFinishRow) {
            RepzLiveWorkoutFinishButton(
                finishLabel,
                isFinishEnabled,
                onFinishClick,
                modifier = Modifier.fillMaxWidth().padding(top = RepzSpacing.sm).then(finishButtonModifier),
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
    nameContentDescription: String,
    modifier: Modifier = Modifier,
    nameModifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (isEditingName) {
            RepzLiveWorkoutNameField(name, onNameChange, onNameEditingFinish, modifier = nameModifier)
        } else {
            Row(
                modifier =
                    nameModifier
                        .clickable(onClick = onNameClick)
                        .semantics(mergeDescendants = true) { contentDescription = nameContentDescription },
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

@Composable
private fun RepzLiveWorkoutNameField(
    name: String,
    onNameChange: (String) -> Unit,
    onNameEditingFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var hasBeenFocused by remember { mutableStateOf(false) }
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = name,
        onValueChange = onNameChange,
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    // onFocusChanged fires once on attach with the initial unfocused state, before
                    // requestFocus() below runs; only a real focused-to-unfocused transition ends editing.
                    if (focusState.isFocused) {
                        hasBeenFocused = true
                    } else if (hasBeenFocused) {
                        onNameEditingFinish()
                    }
                },
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
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { isExpanded = true }, modifier = buttonModifier) {
            Icon(painter = painterResource(Res.drawable.symbol_more_vert), contentDescription = contentDescription)
        }
        RepzWorkoutDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }, items = items)
    }
}
