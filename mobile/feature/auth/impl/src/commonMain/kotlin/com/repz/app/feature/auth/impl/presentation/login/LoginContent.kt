package com.repz.app.feature.auth.impl.presentation.login

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
import com.repz.app.feature.auth.impl.generated.resources.Res
import com.repz.app.feature.auth.impl.generated.resources.auth_email_label
import com.repz.app.feature.auth.impl.generated.resources.auth_log_in
import com.repz.app.feature.auth.impl.generated.resources.auth_password_label
import com.repz.app.feature.auth.impl.generated.resources.error_email_invalid
import com.repz.app.feature.auth.impl.generated.resources.error_email_taken
import com.repz.app.feature.auth.impl.generated.resources.error_password_missing
import com.repz.app.feature.auth.impl.generated.resources.error_password_too_long
import com.repz.app.feature.auth.impl.generated.resources.error_password_too_short
import com.repz.app.feature.auth.impl.generated.resources.login_invalid_credentials
import com.repz.app.feature.auth.impl.generated.resources.login_offline_body
import com.repz.app.feature.auth.impl.generated.resources.login_offline_title
import com.repz.app.feature.auth.impl.generated.resources.login_submitting
import com.repz.app.feature.auth.impl.generated.resources.login_title
import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Stateless (ADR-0005), board 07: top bar with back, the offline banner directly below it (not in
 * the scrollable body, unlike Register's), scrollable fields, a pinned Log in CTA. Wrong
 * credentials draw both fields in error with one message under the password (board 07).
 */
@Composable
fun LoginContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    onBackClick: () -> Unit,
    focusRequesters: Map<LoginField, FocusRequester>,
    snackbarHostState: RepzSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        RepzTopAppBar(onBackClick = onBackClick, backButtonModifier = Modifier.testTag("auth_back"))
        LoginOfflineBanner(state.isOfflineBannerVisible)

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RepzSpacing.xl, vertical = RepzSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg),
        ) {
            Text(stringResource(Res.string.login_title), style = MaterialTheme.typography.headlineLargeEmphasized)
            LoginFields(
                email = state.email,
                onEmailChange = { onIntent(LoginIntent.EmailChanged(it)) },
                onEmailFocusLoss = { onIntent(LoginIntent.EmailFocusLost) },
                emailError = state.emailError,
                password = state.password,
                onPasswordChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                onPasswordFocusLoss = { onIntent(LoginIntent.PasswordFocusLost) },
                isPasswordVisible = state.isPasswordVisible,
                onPasswordVisibilityToggle = { onIntent(LoginIntent.PasswordVisibilityToggled) },
                passwordError = state.passwordError,
                areCredentialsRejected = state.areCredentialsRejected,
                onSubmit = { onIntent(LoginIntent.SubmitClicked) },
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
                stringResource(Res.string.auth_log_in),
                onClick = { onIntent(LoginIntent.SubmitClicked) },
                modifier = Modifier.fillMaxWidth().testTag("login_submit"),
                style = RepzButtonStyle.Filled,
                size = RepzButtonSize.Medium,
                isLoading = state.isSubmitting,
                loadingContentDescription = stringResource(Res.string.login_submitting),
            )
        }
    }
}

@Composable
private fun LoginOfflineBanner(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(RepzMotion.effectsSprings.effectsDefault()),
        exit = fadeOut(RepzMotion.effectsSprings.effectsDefault()),
    ) {
        RepzBanner(
            icon = RepzIcon.CloudOff,
            title = stringResource(Res.string.login_offline_title),
            body = stringResource(Res.string.login_offline_body),
            modifier = Modifier.padding(horizontal = RepzSpacing.xl, vertical = RepzSpacing.md).testTag("auth_offline_banner"),
        )
    }
}

@Composable
private fun ColumnScope.LoginFields(
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
    areCredentialsRejected: Boolean,
    onSubmit: () -> Unit,
    focusRequesters: Map<LoginField, FocusRequester>,
) {
    // A tight group per field (DESIGN_SYSTEM §9): the error message is its own element below the
    // field, not the field's `supportingText` (RepzFieldError's doc explains why), so it needs its
    // own small gap here instead of the outer fields column's normal item spacing.
    val emailErrorMessage = emailError?.message()
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        RepzTextField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(Res.string.auth_email_label),
            isError = emailError != null || areCredentialsRejected,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { focusRequesters.getValue(LoginField.Password).requestFocus() }),
            onFocusLoss = onEmailFocusLoss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters.getValue(LoginField.Email))
                    .semantics { contentType = ContentType.Username }
                    .testTag("login_email"),
        )
        if (emailErrorMessage != null) {
            RepzFieldError(emailErrorMessage, modifier = Modifier.padding(start = RepzSpacing.lg))
        }
    }

    val passwordErrorMessage =
        passwordError?.message()
            ?: if (areCredentialsRejected) stringResource(Res.string.login_invalid_credentials) else null
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
        RepzPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(Res.string.auth_password_label),
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle,
            isError = passwordError != null || areCredentialsRejected,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            onFocusLoss = onPasswordFocusLoss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters.getValue(LoginField.Password))
                    .semantics { contentType = ContentType.Password }
                    .testTag("login_password"),
        )
        if (passwordErrorMessage != null) {
            RepzFieldError(passwordErrorMessage, modifier = Modifier.padding(start = RepzSpacing.lg))
        }
    }
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
