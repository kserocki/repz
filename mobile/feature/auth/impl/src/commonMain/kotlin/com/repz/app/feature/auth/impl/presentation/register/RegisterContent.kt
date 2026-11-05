package com.repz.app.feature.auth.impl.presentation.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
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
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                RegisterFields(state, onIntent, focusRequesters)
            }

            Column(
                modifier =
                    Modifier
                        .padding(horizontal = RepzSpacing.xl)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = RepzSpacing.xl),
            ) {
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

        RepzSnackbarHost(
            state = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).imePadding().navigationBarsPadding(),
        )
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
    state: RegisterUiState,
    onIntent: (RegisterIntent) -> Unit,
    focusRequesters: Map<RegisterField, FocusRequester>,
) {
    RepzTextField(
        value = state.displayName,
        onValueChange = { onIntent(RegisterIntent.DisplayNameChanged(it)) },
        label = stringResource(Res.string.register_display_name_label),
        errorText = state.displayNameError?.message(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusRequesters.getValue(RegisterField.Email).requestFocus() }),
        onFocusLoss = { onIntent(RegisterIntent.DisplayNameFocusLost) },
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequesters.getValue(RegisterField.DisplayName))
                .testTag("register_display_name"),
    )

    RepzTextField(
        value = state.email,
        onValueChange = { onIntent(RegisterIntent.EmailChanged(it)) },
        label = stringResource(Res.string.auth_email_label),
        errorText = state.emailError?.message(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onNext = { focusRequesters.getValue(RegisterField.Password).requestFocus() }),
        onFocusLoss = { onIntent(RegisterIntent.EmailFocusLost) },
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequesters.getValue(RegisterField.Email))
                .semantics { contentType = ContentType.EmailAddress }
                .testTag("register_email"),
    )

    RepzPasswordField(
        value = state.password,
        onValueChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
        label = stringResource(Res.string.auth_password_label),
        isPasswordVisible = state.isPasswordVisible,
        onPasswordVisibilityToggle = { onIntent(RegisterIntent.PasswordVisibilityToggled) },
        supportingText = if (state.passwordError == null) registerPasswordHint() else null,
        errorText = state.passwordError?.message(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onIntent(RegisterIntent.SubmitClicked) }),
        onFocusLoss = { onIntent(RegisterIntent.PasswordFocusLost) },
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequesters.getValue(RegisterField.Password))
                .semantics { contentType = ContentType.NewPassword }
                .testTag("register_password"),
    )
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
