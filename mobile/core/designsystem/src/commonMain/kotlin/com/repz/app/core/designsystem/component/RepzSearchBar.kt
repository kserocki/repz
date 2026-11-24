package com.repz.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.theme.RepzShapes
import com.repz.app.core.designsystem.theme.RepzSpacing

private val SearchBarMinHeight = 56.dp
private const val MAX_QUERY_LENGTH = 100

/**
 * The search field (DESIGN_SYSTEM §9, board 04 §18 / board 10): a pill-shaped
 * `surfaceContainerHigh` bar at least 56 dp tall, a 48 dp leading icon (decorative, or an
 * `IconButton` when [onLeadingClick] is given) and a 48 dp trailing [RepzIcon.Cancel] that clears
 * the query and keeps focus, shown only while [query] is non-empty.
 *
 * Input is capped at [MAX_QUERY_LENGTH] characters (truncated, not refused). The component holds
 * no query state of its own: the caller keeps [query] in its own state (local `remember`, not a
 * ViewModel flow, to avoid the "async state drops keystrokes" gotcha) and forwards it here.
 * [fieldModifier] lands on the `BasicTextField` itself (e.g. a test tag), [modifier] on the bar.
 * [leadingContentDescription] is required when [onLeadingClick] is non-null; a `null`
 * [onLeadingClick] renders a purely decorative icon with no content description.
 */
@Composable
fun RepzSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    leadingIcon: RepzIcon = RepzIcon.Search,
    onLeadingClick: (() -> Unit)? = null,
    leadingContentDescription: String? = null,
    onSearch: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier =
            modifier
                .heightIn(min = SearchBarMinHeight)
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RepzShapes.full)
                .padding(start = RepzSpacing.xs, end = RepzSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
    ) {
        SearchBarLeading(icon = leadingIcon, onClick = onLeadingClick, contentDescription = leadingContentDescription)

        BasicTextField(
            value = query,
            onValueChange = { onQueryChange(it.take(MAX_QUERY_LENGTH)) },
            modifier = fieldModifier.weight(1f).focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                innerTextField()
            },
        )

        if (query.isNotEmpty()) {
            IconButton(
                onClick = {
                    onQueryChange("")
                    focusRequester.requestFocus()
                },
            ) {
                RepzIcon(
                    icon = RepzIcon.Cancel,
                    contentDescription = clearContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchBarLeading(
    icon: RepzIcon,
    onClick: (() -> Unit)?,
    contentDescription: String?,
) {
    if (onClick != null) {
        IconButton(onClick = onClick) {
            RepzIcon(icon = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        Box(modifier = Modifier.size(RepzSpacing.xxxl), contentAlignment = Alignment.Center) {
            RepzIcon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
