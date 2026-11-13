package com.repz.app.core.designsystem.component.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.symbol_add
import com.repz.app.core.designsystem.generated.resources.symbol_more_vert
import com.repz.app.core.designsystem.generated.resources.symbol_sticky_note_2
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource

// DESIGN_SYSTEM §9 "disabled ... 38% content" (board 04), same rule RepzButton applies.
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * One exercise's card (DESIGN_SYSTEM §9 `ExerciseCard`, board 05/09): header with an overflow menu
 * (Reorder · Notes · Remove, a header long-press also calls [onHeaderLongClick] to reorder), an
 * optional notes row ([notes] `null` hides it; a non-null, possibly empty, string shows it,
 * editable while [isEditingNotes]), the column header and [sets], which receives the same
 * Previous-column visibility the header just used, so a caller's rows never disagree with it.
 * [menuButtonModifier]/[notesModifier]/[addSetModifier] tag those elements individually since
 * [modifier] lands on the card.
 */
@Composable
fun RepzExerciseCard(
    name: String,
    subtitle: String,
    notes: String?,
    isEditingNotes: Boolean,
    notesLabel: String,
    onNotesClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onNotesEditingFinish: () -> Unit,
    menuItems: ImmutableList<RepzMenuItem>,
    menuContentDescription: String,
    onHeaderLongClick: () -> Unit,
    columnLabels: RepzSetColumnLabels,
    addSetLabel: String,
    isAddSetEnabled: Boolean,
    onAddSetClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuButtonModifier: Modifier = Modifier,
    notesModifier: Modifier = Modifier,
    addSetModifier: Modifier = Modifier,
    sets: @Composable ColumnScope.(isPreviousColumnVisible: Boolean) -> Unit,
) {
    val isPreviousColumnVisible = rememberIsSetPreviousColumnVisible()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(RepzSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepzSpacing.md),
    ) {
        ExerciseCardHeader(
            name = name,
            subtitle = subtitle,
            menuItems = menuItems,
            menuContentDescription = menuContentDescription,
            onHeaderLongClick = onHeaderLongClick,
            menuButtonModifier = menuButtonModifier,
        )
        if (notes != null) {
            ExerciseCardNotes(
                notes = notes,
                isEditingNotes = isEditingNotes,
                notesLabel = notesLabel,
                onNotesClick = onNotesClick,
                onNotesChange = onNotesChange,
                onNotesEditingFinish = onNotesEditingFinish,
                notesModifier = notesModifier,
            )
        }
        ExerciseCardColumnHeader(labels = columnLabels, isPreviousColumnVisible = isPreviousColumnVisible)
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) { sets(isPreviousColumnVisible) }
        ExerciseCardAddSet(label = addSetLabel, isEnabled = isAddSetEnabled, onClick = onAddSetClick, modifier = addSetModifier)
    }
}

@Composable
private fun ExerciseCardHeader(
    name: String,
    subtitle: String,
    menuItems: ImmutableList<RepzMenuItem>,
    menuContentDescription: String,
    onHeaderLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuButtonModifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .pointerInput(onHeaderLongClick) { detectTapGestures(onLongPress = { onHeaderLongClick() }) },
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { isMenuExpanded = true }, modifier = menuButtonModifier) {
                Icon(painter = painterResource(Res.drawable.symbol_more_vert), contentDescription = menuContentDescription)
            }
            RepzWorkoutDropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }, items = menuItems)
        }
    }
}

@Composable
private fun ExerciseCardNotes(
    notes: String,
    isEditingNotes: Boolean,
    notesLabel: String,
    onNotesClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onNotesEditingFinish: () -> Unit,
    modifier: Modifier = Modifier,
    notesModifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        Icon(
            painter = painterResource(Res.drawable.symbol_sticky_note_2),
            contentDescription = null,
            modifier = Modifier.size(RepzIconSize.dense),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isEditingNotes) {
            ExerciseCardNotesField(
                notes,
                notesLabel,
                onNotesChange,
                onNotesEditingFinish,
                modifier = Modifier.weight(1f).then(notesModifier),
            )
        } else {
            Text(
                text = notes,
                modifier = Modifier.weight(1f).then(notesModifier).clickable(onClick = onNotesClick),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExerciseCardNotesField(
    notes: String,
    notesLabel: String,
    onNotesChange: (String) -> Unit,
    onNotesEditingFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var hasBeenFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = notes,
        onValueChange = onNotesChange,
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    // onFocusChanged fires once on attach with the initial unfocused state, before
                    // requestFocus() below runs; only a real focused-to-unfocused transition ends editing.
                    if (focusState.isFocused) {
                        hasBeenFocused = true
                    } else if (hasBeenFocused) {
                        onNotesEditingFinish()
                    }
                },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        // Clearing focus (not calling onNotesEditingFinish directly) routes Done through the same
        // focused-to-unfocused path above, so editing ends exactly once however it happens.
        keyboardActions =
            KeyboardActions(
                onDone = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                },
            ),
        decorationBox = { innerTextField ->
            if (notes.isEmpty()) {
                Text(text = notesLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            innerTextField()
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** Mirrors [RepzSetRow]'s own reflow at [REFLOW_FONT_SCALE_THRESHOLD] (board 14): below it, the
 * single-line header shows "Prev" exactly when [isPreviousColumnVisible] is `true` (the same
 * decision every row below it gets); at or above it, Prev and the badge column's label both drop,
 * matching the reflowed row's bare badge. */
@Composable
private fun ExerciseCardColumnHeader(
    labels: RepzSetColumnLabels,
    isPreviousColumnVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (LocalDensity.current.fontScale >= REFLOW_FONT_SCALE_THRESHOLD) {
        ExerciseCardColumnHeaderReflowed(labels, modifier)
    } else {
        ExerciseCardColumnHeaderSingleLine(labels, isPreviousColumnVisible, modifier)
    }
}

@Composable
private fun ExerciseCardColumnHeaderSingleLine(
    labels: RepzSetColumnLabels,
    isPreviousColumnVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = SetRowStartPadding, end = RepzSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SetColumnGap),
    ) {
        ColumnHeaderLabel(text = labels.set, modifier = Modifier.width(SetBadgeColumnWidth))
        if (isPreviousColumnVisible) {
            ColumnHeaderLabel(text = labels.previous, modifier = Modifier.width(SetPreviousColumnWidth))
        }
        ColumnHeaderLabel(text = labels.weight, modifier = Modifier.weight(1f))
        ColumnHeaderLabel(text = labels.reps, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(SetCheckColumnWidth))
    }
}

@Composable
private fun ExerciseCardColumnHeaderReflowed(
    labels: RepzSetColumnLabels,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = SetRowStartPadding, end = RepzSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SetColumnGap),
    ) {
        Spacer(modifier = Modifier.width(SetBadgeColumnWidth))
        ColumnHeaderLabel(text = labels.weight, modifier = Modifier.weight(1f))
        ColumnHeaderLabel(text = labels.reps, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(SetCheckColumnWidth))
    }
}

@Composable
private fun ColumnHeaderLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text, modifier = modifier, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ExerciseCardAddSet(
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (isEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
        }
    Row(
        modifier =
            modifier
                .heightIn(min = RepzSpacing.xxxl)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = isEnabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
    ) {
        Icon(
            painter = painterResource(Res.drawable.symbol_add),
            contentDescription = null,
            modifier = Modifier.size(RepzIconSize.dense),
            tint = contentColor,
        )
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}
