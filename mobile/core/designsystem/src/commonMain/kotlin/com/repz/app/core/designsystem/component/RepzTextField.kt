package com.repz.app.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
 * [errorText] replaces [supportingText] with the error copy prefixed by a 16 dp error icon
 * (`RepzIconSize.inline`), appearing instantly and fading in over [RepzMotion.tweens.short]
 * (MOTION.md §1). [isError] alone (no [errorText]) draws the error outline without replacing the
 * supporting text, for a field whose error is shown elsewhere (e.g. the email field when
 * credentials are rejected on Login). [onFocusLoss] fires only after the field has gained focus at
 * least once.
 */
@Composable
fun RepzTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusLoss: (() -> Unit)? = null,
) {
    RepzBaseTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        supportingText = supportingText,
        errorText = errorText,
        isError = isError,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        onFocusLoss = onFocusLoss,
        visualTransformation = VisualTransformation.None,
        trailingIcon = null,
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
    errorText: String? = null,
    isError: Boolean = errorText != null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusLoss: (() -> Unit)? = null,
) {
    RepzBaseTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        supportingText = supportingText,
        errorText = errorText,
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
    )
}

@Composable
private fun RepzBaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String?,
    errorText: String?,
    isError: Boolean,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    onFocusLoss: (() -> Unit)?,
    visualTransformation: VisualTransformation,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)?,
) {
    var wasFocused by remember { mutableStateOf(false) }
    val supportingTextSlot: (@Composable () -> Unit)? = fieldSupportingTextSlot(supportingText, errorText)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier.onFocusChanged { focusState ->
                if (!focusState.isFocused && wasFocused) {
                    onFocusLoss?.invoke()
                }
                wasFocused = focusState.isFocused
            },
        enabled = enabled,
        label = { Text(label) },
        supportingText = supportingTextSlot,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = MaterialTheme.shapes.small,
    )
}

/** `errorText` replaces `supportingText` (animated, see [RepzFieldErrorText]); with neither, there is
 * no supporting-text slot at all. */
private fun fieldSupportingTextSlot(
    supportingText: String?,
    errorText: String?,
): (@Composable () -> Unit)? =
    when {
        errorText != null -> {
            { RepzFieldErrorText(errorText) }
        }

        supportingText != null -> {
            { Text(supportingText) }
        }

        else -> {
            null
        }
    }

/** The error row: icon + text, instant then fading in over [RepzMotion.tweens.short] (MOTION.md
 * §1). Keyed by [errorText] so a new error message restarts the fade. */
@Composable
private fun RepzFieldErrorText(errorText: String) {
    key(errorText) {
        val alpha = remember { Animatable(0f) }
        val fadeInSpec = RepzMotion.tweens.short<Float>()
        LaunchedEffect(Unit) { alpha.animateTo(1f, fadeInSpec) }
        Row(
            modifier = Modifier.alpha(alpha.value),
            horizontalArrangement = Arrangement.spacedBy(RepzSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepzIcon(
                icon = RepzIcon.Error,
                contentDescription = null,
                modifier = Modifier.size(RepzIconSize.inline),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(errorText)
        }
    }
}
