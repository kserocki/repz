package com.repz.app.feature.auth.impl.presentation.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.repz.app.core.designsystem.component.RepzBanner
import com.repz.app.core.designsystem.component.RepzButton
import com.repz.app.core.designsystem.component.RepzButtonSize
import com.repz.app.core.designsystem.component.RepzButtonStyle
import com.repz.app.core.designsystem.component.RepzFieldError
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzPasswordField
import com.repz.app.core.designsystem.component.RepzSnackbarHost
import com.repz.app.core.designsystem.component.RepzSnackbarHostState
import com.repz.app.core.designsystem.component.RepzTextField
import com.repz.app.core.designsystem.component.RepzTopAppBar
import com.repz.app.core.designsystem.motion.RepzMotion
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.domain.model.account.Password
import com.repz.app.feature.auth.impl.generated.resources.Res
import com.repz.app.feature.auth.impl.generated.resources.auth_create_account
import com.repz.app.feature.auth.impl.generated.resources.auth_email_label
import com.repz.app.feature.auth.impl.generated.resources.auth_password_label
import com.repz.app.feature.auth.impl.generated.resources.error_display_name_blank
import com.repz.app.feature.auth.impl.generated.resources.error_display_name_too_long
import com.repz.app.feature.auth.impl.generated.resources.error_email_invalid
import com.repz.app.feature.auth.impl.generated.resources.error_email_taken
import com.repz.app.feature.auth.impl.generated.resources.error_password_missing
import com.repz.app.feature.auth.impl.generated.resources.error_password_too_long
import com.repz.app.feature.auth.impl.generated.resources.error_password_too_short
import com.repz.app.feature.auth.impl.generated.resources.register_display_name_label
import com.repz.app.feature.auth.impl.generated.resources.register_offline_banner
import com.repz.app.feature.auth.impl.generated.resources.register_password_hint
import com.repz.app.feature.auth.impl.generated.resources.register_submitting
import com.repz.app.feature.auth.impl.generated.resources.register_subtitle
import com.repz.app.feature.auth.impl.generated.resources.register_title
import com.repz.app.feature.auth.impl.presentation.common.DisplayNameFieldError
import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Stateless (ADR-0005), board 07: top bar with back, scrollable body (title, subtitle, fields),
 * a pinned Create account CTA above the keyboard/navigation bar. The offline banner fades in/out
 * on [RepzMotion.effectsSprings]'s default effects spring, per MOTION.md §1; nothing else animates.
 */
@Composable
fun RegisterContent(
    state: RegisterUiState,
    onIntent: (RegisterIntent) -> Unit,
    onBackClick: () -> Unit,
    focusRequesters: Map<RegisterField, FocusRequester>,
    snackbarHostState: RepzSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        RepzTopAppBar(onBackClick = onBackClick, backButtonModifier = Modifier.testTag("auth_back"))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RepzSpacing.xl, vertical = RepzSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg),
        ) {
            RegisterOfflineBanner(state.isOfflineBannerVisible)
            Text(stringResource(Res.string.register_title), style = MaterialTheme.typography.headlineLargeEmphasized)
            Text(
                stringResource(Res.string.register_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RegisterFields(
                displayName = state.displayName,
                onDisplayNameChange = { onIntent(RegisterIntent.DisplayNameChanged(it)) },
                onDisplayNameFocusLoss = { onIntent(RegisterIntent.DisplayNameFocusLost) },
                displayNameError = state.displayNameError,
                email = state.email,
                onEmailChange = { onIntent(RegisterIntent.EmailChanged(it)) },
                onEmailFocusLoss = { onIntent(RegisterIntent.EmailFocusLost) },
                emailError = state.emailError,
                password = state.password,
                onPasswordChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
                onPasswordFocusLoss = { onIntent(RegisterIntent.PasswordFocusLost) },
                isPasswordVisible = state.isPasswordVisible,
                onPasswordVisibilityToggle = { onIntent(RegisterIntent.PasswordVisibilityToggled) },
                passwordError = state.passwordError,
                onSubmit = { onIntent(RegisterIntent.SubmitClicked) },
                focusRequesters = focusRequesters,
            )
        }

        Column(
            modifier =
                Modifier
                    .padding(horizontal = RepzSpacing.xl)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = RepzSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RepzSpacing.sm),
        ) {
            // Above the CTA, not overlaid on top of it (board 07): a shown snackbar pushes the
            // button up instead of covering it.
            RepzSnackbarHost(state = snackbarHostState, modifier = Modifier.fillMaxWidth())
            RepzButton(
                stringResource(Res.string.auth_create_account),
                onClick = { onIntent(RegisterIntent.SubmitClicked) },
                modifier = Modifier.fillMaxWidth().testTag("register_submit"),
                style = RepzButtonStyle.Filled,
                size = RepzButtonSize.Medium,
                isLoading = state.isSubmitting,
                loadingContentDescription = stringResource(Res.string.register_submitting),
            )
        }
    }
}

@Composable
private fun RegisterOfflineBanner(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(RepzMotion.effectsSprings.effectsDefault()),
        exit = fadeOut(RepzMotion.effectsSprings.effectsDefault()),
    ) {
        RepzBanner(
            icon = RepzIcon.CloudOff,
            title = stringResource(Res.string.register_offline_banner),
            modifier = Modifier.testTag("auth_offline_banner"),
        )
    }
}

@Composable
private fun ColumnScope.RegisterFields(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onDisplayNameFocusLoss: () -> Unit,
    displayNameError: DisplayNameFieldError?,
    email: String,
    onEmailChange: (String) -> Unit,
    onEmailFocusLoss: () -> Unit,
    emailError: EmailFieldError?,
    password: String,
    onPasswordChange: (String) -> Unit,
    onPasswordFocusLoss: () -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    passwordError: PasswordFieldError?,
    onSubmit: () -> Unit,
    focusRequesters: Map<RegisterField, FocusRequester>,
) {
    RegisterDisplayNameField(
        displayName = displayName,
        onDisplayNameChange = onDisplayNameChange,
        onDisplayNameFocusLoss = onDisplayNameFocusLoss,
        error = displayNameError,
        focusRequesters = focusRequesters,
    )
    RegisterEmailField(
        email = email,
        onEmailChange = onEmailChange,
        onEmailFocusLoss = onEmailFocusLoss,
        error = emailError,
        focusRequesters = focusRequesters,
    )
    RegisterPasswordField(
        password = password,
        onPasswordChange = onPasswordChange,
        onPasswordFocusLoss = onPasswordFocusLoss,
        isPasswordVisible = isPasswordVisible,
        onPasswordVisibilityToggle = onPasswordVisibilityToggle,
        error = passwordError,
        onSubmit = onSubmit,
        focusRequesters = focusRequesters,
    )
}

// A tight group per field (DESIGN_SYSTEM §9): the error message is its own element below the
// field, not the field's `supportingText` (RepzFieldError's doc explains why), so each needs its
// own small gap here instead of the outer fields column's normal item spacing.

@Composable
private fun ColumnScope.RegisterDisplayNameField(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onDisplayNameFocusLoss: () -> Unit,
    error: DisplayNameFieldError?,
    focusRequesters: Map<RegisterField, FocusRequester>,
) {
    val errorMessage = error?.message()
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        RepzTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = stringResource(Res.string.register_display_name_label),
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusRequesters.getValue(RegisterField.Email).requestFocus() }),
            onFocusLoss = onDisplayNameFocusLoss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters.getValue(RegisterField.DisplayName))
                    .testTag("register_display_name"),
        )
        if (errorMessage != null) {
            RepzFieldError(errorMessage, modifier = Modifier.padding(start = RepzSpacing.lg))
        }
    }
}

@Composable
private fun ColumnScope.RegisterEmailField(
    email: String,
    onEmailChange: (String) -> Unit,
    onEmailFocusLoss: () -> Unit,
    error: EmailFieldError?,
    focusRequesters: Map<RegisterField, FocusRequester>,
) {
    val errorMessage = error?.message()
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        RepzTextField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(Res.string.auth_email_label),
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { focusRequesters.getValue(RegisterField.Password).requestFocus() }),
            onFocusLoss = onEmailFocusLoss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters.getValue(RegisterField.Email))
                    .semantics { contentType = ContentType.EmailAddress }
                    .testTag("register_email"),
        )
        if (errorMessage != null) {
            RepzFieldError(errorMessage, modifier = Modifier.padding(start = RepzSpacing.lg))
        }
    }
}

@Composable
private fun ColumnScope.RegisterPasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    onPasswordFocusLoss: () -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    error: PasswordFieldError?,
    onSubmit: () -> Unit,
    focusRequesters: Map<RegisterField, FocusRequester>,
) {
    val errorMessage = error?.message()
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        RepzPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(Res.string.auth_password_label),
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle,
            supportingText = if (errorMessage == null) registerPasswordHint() else null,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            onFocusLoss = onPasswordFocusLoss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters.getValue(RegisterField.Password))
                    .semantics { contentType = ContentType.NewPassword }
                    .testTag("register_password"),
        )
        if (errorMessage != null) {
            RepzFieldError(errorMessage, modifier = Modifier.padding(start = RepzSpacing.lg))
        }
    }
}

@Composable
private fun registerPasswordHint(): String =
    pluralStringResource(Res.plurals.register_password_hint, Password.NEW_PASSWORD_LENGTH.first, Password.NEW_PASSWORD_LENGTH.first)

@Composable
private fun DisplayNameFieldError.message(): String =
    when (this) {
        DisplayNameFieldError.Blank -> stringResource(Res.string.error_display_name_blank)
        is DisplayNameFieldError.TooLong -> pluralStringResource(Res.plurals.error_display_name_too_long, maxLength, maxLength)
    }

@Composable
private fun EmailFieldError.message(): String =
    when (this) {
        EmailFieldError.Invalid -> stringResource(Res.string.error_email_invalid)
        EmailFieldError.Taken -> stringResource(Res.string.error_email_taken)
    }

@Composable
private fun PasswordFieldError.message(): String =
    when (this) {
        PasswordFieldError.Missing -> stringResource(Res.string.error_password_missing)
        is PasswordFieldError.TooShort -> pluralStringResource(Res.plurals.error_password_too_short, minLength, minLength)
        is PasswordFieldError.TooLong -> pluralStringResource(Res.plurals.error_password_too_long, maxLength, maxLength)
    }
