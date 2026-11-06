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
 * One exercise's card in the live workout (DESIGN_SYSTEM §9 `ExerciseCard`, board 05/09):
 * `shapes.large` (16 dp), `surfaceContainer` (level2) container. [name]/[subtitle] and the overflow
 * menu ([menuItems], Q7: Reorder · Notes · Remove) sit in the header; a long press anywhere on the
 * header calls [onHeaderLongClick] to enter reorder mode (board 09 §10b). [notes] is `null` to hide
 * the notes row entirely, or a (possibly empty, while [isEditingNotes]) string to show it inline
 * under the header (Q9). [columnLabels] labels the columns above [sets], which are [RepzSetRow]s
 * supplied by the caller.
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
    sets: @Composable ColumnScope.() -> Unit,
) {
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
        )
        if (notes != null) {
            ExerciseCardNotes(
                notes = notes,
                isEditingNotes = isEditingNotes,
                notesLabel = notesLabel,
                onNotesClick = onNotesClick,
                onNotesChange = onNotesChange,
                onNotesEditingFinish = onNotesEditingFinish,
            )
        }
        ExerciseCardColumnHeader(columnLabels)
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs), content = sets)
        ExerciseCardAddSet(label = addSetLabel, isEnabled = isAddSetEnabled, onClick = onAddSetClick)
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
            IconButton(onClick = { isMenuExpanded = true }) {
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
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm)) {
        Icon(
            painter = painterResource(Res.drawable.symbol_sticky_note_2),
            contentDescription = null,
            modifier = Modifier.size(RepzIconSize.dense),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isEditingNotes) {
            ExerciseCardNotesField(notes, notesLabel, onNotesChange, onNotesEditingFinish, modifier = Modifier.weight(1f))
        } else {
            Text(
                text = notes,
                modifier = Modifier.weight(1f).clickable(onClick = onNotesClick),
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
    BasicTextField(
        value = notes,
        onValueChange = onNotesChange,
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState -> if (!focusState.isFocused) onNotesEditingFinish() },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (notes.isEmpty()) {
                Text(text = notesLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            innerTextField()
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun ExerciseCardColumnHeader(
    labels: RepzSetColumnLabels,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = SetRowStartPadding, end = RepzSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SetColumnGap),
    ) {
        ColumnHeaderLabel(text = labels.set, modifier = Modifier.width(SetBadgeColumnWidth))
        ColumnHeaderLabel(text = labels.previous, modifier = Modifier.width(SetPreviousColumnWidth))
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
