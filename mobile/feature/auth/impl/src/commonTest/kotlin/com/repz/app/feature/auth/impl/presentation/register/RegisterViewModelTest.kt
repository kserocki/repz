package com.repz.app.feature.auth.impl.presentation.register

import app.cash.turbine.test
import com.repz.app.core.domain.auth.AccountField
import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.testing.auth.FakeAuthRepository
import com.repz.app.feature.auth.impl.domain.RegisterUseCase
import com.repz.app.feature.auth.impl.presentation.common.DisplayNameFieldError
import com.repz.app.feature.auth.impl.presentation.common.EmailFieldError
import com.repz.app.feature.auth.impl.presentation.common.PasswordFieldError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private val FIXTURE_USER =
    User(
        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
        email = "alex@repz.app",
        displayName = "Alex",
        unitSystem = UnitSystem.KG,
    )
private const val VALID_DISPLAY_NAME = "Alex"
private const val VALID_EMAIL = "alex@repz.app"
private const val VALID_PASSWORD = "correct-horse-battery"

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeAuthRepository = FakeAuthRepository()) = RegisterViewModel(RegisterUseCase(repository))

    private fun RegisterViewModel.fillValidFields() {
        onIntent(RegisterIntent.DisplayNameChanged(VALID_DISPLAY_NAME))
        onIntent(RegisterIntent.EmailChanged(VALID_EMAIL))
        onIntent(RegisterIntent.PasswordChanged(VALID_PASSWORD))
    }

    @Test
    fun given_newViewModel_whenCreated_thenStateIsEmptyAndIdle() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            assertEquals(RegisterUiState(), viewModel.state.value)
        }

    @Test
    fun given_noShownError_whenDisplayNameChanged_thenNoErrorAppears() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(RegisterIntent.DisplayNameChanged(""))

            assertEquals(null, viewModel.state.value.displayNameError)
        }

    @Test
    fun given_shownPasswordError_whenPasswordChanged_thenRevalidatesImmediately() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(RegisterIntent.PasswordChanged("short"))
            viewModel.onIntent(RegisterIntent.PasswordFocusLost)
            assertEquals(PasswordFieldError.TooShort(8), viewModel.state.value.passwordError)

            viewModel.onIntent(RegisterIntent.PasswordChanged(VALID_PASSWORD))

            assertEquals(null, viewModel.state.value.passwordError)
        }

    @Test
    fun given_untouchedEmptyDisplayName_whenFocusLost_thenStaysQuiet() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(RegisterIntent.DisplayNameFocusLost)

            assertEquals(null, viewModel.state.value.displayNameError)
        }

    @Test
    fun given_blankDisplayName_whenFocusLost_thenShowsBlankError() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(RegisterIntent.DisplayNameChanged("   "))

            viewModel.onIntent(RegisterIntent.DisplayNameFocusLost)

            assertEquals(DisplayNameFieldError.Blank, viewModel.state.value.displayNameError)
        }

    @Test
    fun given_shortPassword_whenFocusLost_thenShowsTooShortError() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(RegisterIntent.PasswordChanged("short"))

            viewModel.onIntent(RegisterIntent.PasswordFocusLost)

            assertEquals(PasswordFieldError.TooShort(8), viewModel.state.value.passwordError)
        }

    @Test
    fun given_passwordVisibilityToggled_whenToggledAgain_thenFlipsBothTimes() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(RegisterIntent.PasswordVisibilityToggled)
            assertTrue(viewModel.state.value.isPasswordVisible)

            viewModel.onIntent(RegisterIntent.PasswordVisibilityToggled)
            assertEquals(false, viewModel.state.value.isPasswordVisible)
        }

    @Test
    fun given_allFieldsInvalid_whenSubmitClicked_thenShowsErrorsFocusesDisplayNameFirstAndNeverCallsTheRepository() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onIntent(RegisterIntent.SubmitClicked)

                assertEquals(RegisterEffect.FocusField(RegisterField.DisplayName), awaitItem())
            }
            val state = viewModel.state.value
            assertEquals(DisplayNameFieldError.Blank, state.displayNameError)
            assertEquals(EmailFieldError.Invalid, state.emailError)
            assertEquals(PasswordFieldError.TooShort(8), state.passwordError)
            assertTrue(repository.registerCalls.isEmpty())
        }

    @Test
    fun given_onlyPasswordInvalid_whenSubmitClicked_thenFocusesPassword() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(RegisterIntent.DisplayNameChanged(VALID_DISPLAY_NAME))
            viewModel.onIntent(RegisterIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(RegisterIntent.PasswordChanged("short"))

            viewModel.effects.test {
                viewModel.onIntent(RegisterIntent.SubmitClicked)

                assertEquals(RegisterEffect.FocusField(RegisterField.Password), awaitItem())
            }
        }

    @Test
    fun given_validFields_whenSubmitClicked_thenSetsSubmittingAndCallsTheRepository() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository().apply { registerGate = CompletableDeferred() }
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()

            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.state.value.isSubmitting)
            assertEquals(false, viewModel.state.value.isOfflineBannerVisible)
            assertEquals(1, repository.registerCalls.size)
        }

    @Test
    fun given_submitting_whenSubmitClickedAgain_thenSecondClickIsIgnored() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository().apply { registerGate = CompletableDeferred() }
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()
            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repository.registerCalls.size)
        }

    @Test
    fun given_successfulRegistration_whenSubmitted_thenKeepsSubmittingTrue() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(registerResult = DomainResult.Success(FIXTURE_USER))
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()

            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.state.value.isSubmitting)
        }

    @Test
    fun given_emailTaken_whenSubmitted_thenShowsTakenErrorAndFocusesEmail() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.EmailTaken))
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()

            viewModel.effects.test {
                viewModel.onIntent(RegisterIntent.SubmitClicked)

                assertEquals(RegisterEffect.FocusField(RegisterField.Email), awaitItem())
            }
            val state = viewModel.state.value
            assertEquals(EmailFieldError.Taken, state.emailError)
            assertEquals(false, state.isSubmitting)
        }

    @Test
    fun given_emailTakenError_whenFocusLostWithoutEditing_thenTakenErrorStays() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.EmailTaken))
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()
            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(EmailFieldError.Taken, viewModel.state.value.emailError)

            viewModel.onIntent(RegisterIntent.EmailFocusLost)

            assertEquals(EmailFieldError.Taken, viewModel.state.value.emailError)
        }

    @Test
    fun given_emailTakenError_whenEmailEdited_thenClears() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.EmailTaken))
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()
            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(EmailFieldError.Taken, viewModel.state.value.emailError)

            viewModel.onIntent(RegisterIntent.EmailChanged("$VALID_EMAIL.edited"))

            assertEquals(null, viewModel.state.value.emailError)
        }

    @Test
    fun given_noConnection_whenSubmitted_thenShowsOfflineBanner() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.NoConnection))
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()

            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.isOfflineBannerVisible)
            assertEquals(false, state.isSubmitting)
        }

    @Test
    fun given_fieldsRejectedByServer_whenSubmitted_thenShowsMatchingErrorsAndFocusesFirst() =
        runTest(dispatcher) {
            val repository =
                FakeAuthRepository(
                    registerResult = DomainResult.Failure(AuthError.FieldsRejected(setOf(AccountField.Email, AccountField.Password))),
                )
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()

            viewModel.effects.test {
                viewModel.onIntent(RegisterIntent.SubmitClicked)

                assertEquals(RegisterEffect.FocusField(RegisterField.Email), awaitItem())
            }
            val state = viewModel.state.value
            assertEquals(EmailFieldError.Invalid, state.emailError)
            assertEquals(PasswordFieldError.TooShort(8), state.passwordError)
        }

    @Test
    fun given_fieldsRejectedByServer_whenDisplayNameNowFailsLocalValidation_thenShowsTheLocalError() =
        runTest(dispatcher) {
            val repository =
                FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.FieldsRejected(setOf(AccountField.DisplayName))))
                    .apply { registerGate = CompletableDeferred() }
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()
            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            // Edited while the request is in flight: the new value fails DisplayName.of's max length.
            viewModel.onIntent(RegisterIntent.DisplayNameChanged("a".repeat(51)))
            repository.registerGate?.complete(Unit)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(DisplayNameFieldError.TooLong(50), viewModel.state.value.displayNameError)
        }

    @Test
    fun given_unexpectedError_whenSubmitted_thenEmitsShowUnexpectedErrorEffect() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.Unexpected))
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()

            viewModel.effects.test {
                viewModel.onIntent(RegisterIntent.SubmitClicked)

                assertEquals(RegisterEffect.ShowUnexpectedError, awaitItem())
            }
            assertEquals(false, viewModel.state.value.isSubmitting)
        }

    @Test
    fun given_failedSubmission_whenSubmitClickedAgainLikeARetryTap_thenCallsTheRepositoryASecondTime() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(registerResult = DomainResult.Failure(AuthError.Unexpected))
            val viewModel = viewModel(repository)
            viewModel.fillValidFields()
            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            // The snackbar's Retry action and the field's IME Done action both just resend SubmitClicked.
            viewModel.onIntent(RegisterIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, repository.registerCalls.size)
        }
}
