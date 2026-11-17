package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.repz.app.core.designsystem.generated.resources.Res
import com.repz.app.core.designsystem.generated.resources.designsystem_hide_password
import com.repz.app.core.designsystem.generated.resources.designsystem_show_password
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzIconSize
import com.repz.app.core.designsystem.theme.RepzSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * The single-line outlined field (DESIGN_SYSTEM §9, board 07): height 56 (M3's own
 * [androidx.compose.material3.OutlinedTextFieldDefaults.MinHeight]), `shapes.small` (8), 1 dp
 * `outline` / 2 dp focused `primary` / 2 dp error (M3 defaults, unchanged).
 *
 * [isError] draws the error outline and M3's own generic `error(...)` TalkBack semantics; the error
 * *message* is a separate concern, deliberately not this field's `supportingText` or any other
 * parameter here: render a [RepzFieldError] below it instead (see its doc for why). [onFocusLoss]
 * fires only after the field has gained focus at least once. [style] is
 * [RepzTextFieldStyle.Outlined] unless the caller asks for the board 04 §21 filled box.
 */
@Composable
fun RepzTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusLoss: (() -> Unit)? = null,
    style: RepzTextFieldStyle = RepzTextFieldStyle.Outlined,
) {
    RepzBaseTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        supportingText = supportingText,
        isError = isError,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        onFocusLoss = onFocusLoss,
        visualTransformation = VisualTransformation.None,
        trailingIcon = null,
        style = style,
    )
}

/** [RepzTextField] with a password mask and a 48 dp show/hide toggle (M3's
 * [androidx.compose.material3.IconButton] already enforces the 48 dp minimum touch target). */
@Composable
fun RepzPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusLoss: (() -> Unit)? = null,
    style: RepzTextFieldStyle = RepzTextFieldStyle.Outlined,
) {
    RepzBaseTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        supportingText = supportingText,
        isError = isError,
        enabled = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        onFocusLoss = onFocusLoss,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val icon = if (isPasswordVisible) RepzIcon.VisibilityOff else RepzIcon.Visibility
            val description =
                stringResource(
                    if (isPasswordVisible) Res.string.designsystem_hide_password else Res.string.designsystem_show_password,
                )
            IconButton(onClick = onPasswordVisibilityToggle) {
                RepzIcon(icon = icon, contentDescription = description)
            }
        },
        style = style,
    )
}

@Composable
private fun RepzBaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String?,
    isError: Boolean,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    onFocusLoss: (() -> Unit)?,
    visualTransformation: VisualTransformation,
    style: RepzTextFieldStyle,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)?,
) {
    var wasFocused by remember { mutableStateOf(false) }
    val focusModifier =
        modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused && wasFocused) {
                onFocusLoss?.invoke()
            }
            wasFocused = focusState.isFocused
        }

    when (style) {
        RepzTextFieldStyle.Outlined -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = focusModifier,
                enabled = enabled,
                label = { Text(label) },
                supportingText = supportingText?.let { { Text(it) } },
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                trailingIcon = trailingIcon,
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )
        }

        RepzTextFieldStyle.Filled -> {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = focusModifier,
                enabled = enabled,
                label = { Text(label) },
                supportingText = supportingText?.let { { Text(it) } },
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                trailingIcon = trailingIcon,
                singleLine = true,
                shape = MaterialTheme.shapes.small.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
            )
        }
    }
}

/**
 * A [RepzTextField]/[RepzPasswordField]'s error message (icon + text), placed by the caller
 * directly below the field instead of passing it in as the field's own `supportingText`: M3's
 * `OutlinedTextField` merges label/value/supportingText into one semantics node, and Compose
 * Multiplatform's iOS bridge doesn't expose that merged supportingText while the field is
 * non-blank, so a VoiceOver user would never hear it. As its own element here it is reachable on
 * both platforms, and a polite live region announces it the moment it appears. Pair it with
 * `isError = true` on the field above for the error outline and Android's own TalkBack
 * announcement. Instant, then fades in over [RepzMotion.tweens.short] (MOTION.md §1); keyed by
 * [text] so a new message restarts the fade.
 */
@Composable
fun RepzFieldError(
    text: String,
    modifier: Modifier = Modifier,
) {
    key(text) {
        val alpha = remember { Animatable(0f) }
        val fadeInSpec = RepzMotion.tweens.short<Float>()
        LaunchedEffect(Unit) { alpha.animateTo(1f, fadeInSpec) }
        Row(
            modifier = modifier.alpha(alpha.value).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepzIcon(
                icon = RepzIcon.Error,
                contentDescription = null,
                modifier = Modifier.size(RepzIconSize.inline),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
