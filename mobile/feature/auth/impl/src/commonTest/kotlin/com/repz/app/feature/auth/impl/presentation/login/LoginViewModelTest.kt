package com.repz.app.feature.auth.impl.presentation.login

import app.cash.turbine.test
import com.repz.app.core.domain.auth.AccountField
import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.testing.auth.FakeAuthRepository
import com.repz.app.feature.auth.impl.domain.LogInUseCase
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
private const val VALID_EMAIL = "alex@repz.app"
private const val VALID_PASSWORD = "correct-horse"

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeAuthRepository = FakeAuthRepository()) = LoginViewModel(LogInUseCase(repository))

    @Test
    fun given_newViewModel_whenCreated_thenStateIsEmptyAndIdle() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            assertEquals(LoginUiState(), viewModel.state.value)
        }

    @Test
    fun given_noShownError_whenEmailChanged_thenNoErrorAppears() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(LoginIntent.EmailChanged("not-an-email"))

            assertEquals(LoginUiState(email = "not-an-email"), viewModel.state.value)
        }

    @Test
    fun given_shownEmailError_whenEmailChanged_thenRevalidatesImmediately() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(LoginIntent.EmailChanged("not-an-email"))
            viewModel.onIntent(LoginIntent.EmailFocusLost)
            assertEquals(EmailFieldError.Invalid, viewModel.state.value.emailError)

            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))

            assertEquals(null, viewModel.state.value.emailError)
        }

    @Test
    fun given_untouchedEmptyField_whenFocusLost_thenStaysQuiet() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(LoginIntent.EmailFocusLost)

            assertEquals(null, viewModel.state.value.emailError)
        }

    @Test
    fun given_invalidEmail_whenFocusLost_thenShowsError() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(LoginIntent.EmailChanged("not-an-email"))

            viewModel.onIntent(LoginIntent.EmailFocusLost)

            assertEquals(EmailFieldError.Invalid, viewModel.state.value.emailError)
        }

    @Test
    fun given_touchedThenEmptiedPassword_whenFocusLost_thenStaysQuietWithoutAPriorError() =
        runTest(dispatcher) {
            // Deleting back to empty never showed an error along the way (rule 1), so by the time
            // focus is lost the field looks exactly like an untouched one and stays quiet.
            val viewModel = viewModel()
            viewModel.onIntent(LoginIntent.PasswordChanged("something"))
            viewModel.onIntent(LoginIntent.PasswordChanged(""))

            viewModel.onIntent(LoginIntent.PasswordFocusLost)

            assertEquals(null, viewModel.state.value.passwordError)
        }

    @Test
    fun given_tooLongPassword_whenFocusLost_thenShowsTooLongError() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(LoginIntent.PasswordChanged("x".repeat(129)))

            viewModel.onIntent(LoginIntent.PasswordFocusLost)

            assertEquals(PasswordFieldError.TooLong(128), viewModel.state.value.passwordError)
        }

    @Test
    fun given_passwordVisibilityToggled_whenToggledAgain_thenFlipsBothTimes() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(LoginIntent.PasswordVisibilityToggled)
            assertTrue(viewModel.state.value.isPasswordVisible)

            viewModel.onIntent(LoginIntent.PasswordVisibilityToggled)
            assertEquals(false, viewModel.state.value.isPasswordVisible)
        }

    @Test
    fun given_invalidFields_whenSubmitClicked_thenShowsErrorsFocusesFirstAndNeverCallsTheRepository() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onIntent(LoginIntent.SubmitClicked)

                assertEquals(LoginEffect.FocusField(LoginField.Email), awaitItem())
            }
            assertEquals(EmailFieldError.Invalid, viewModel.state.value.emailError)
            assertEquals(PasswordFieldError.Missing, viewModel.state.value.passwordError)
            assertTrue(repository.logInCalls.isEmpty())
        }

    @Test
    fun given_validFields_whenSubmitClicked_thenSetsSubmittingAndCallsTheRepository() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository().apply { logInGate = CompletableDeferred() }
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))

            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.state.value.isSubmitting)
            assertEquals(false, viewModel.state.value.isOfflineBannerVisible)
            assertEquals(1, repository.logInCalls.size)
        }

    @Test
    fun given_submitting_whenSubmitClickedAgain_thenSecondClickIsIgnored() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository().apply { logInGate = CompletableDeferred() }
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))
            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repository.logInCalls.size)
        }

    @Test
    fun given_successfulLogin_whenSubmitted_thenKeepsSubmittingTrue() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Success(FIXTURE_USER))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))

            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.state.value.isSubmitting)
        }

    @Test
    fun given_invalidCredentials_whenSubmitted_thenShowsCredentialsRejectedAndFocusesPassword() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.InvalidCredentials))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))

            viewModel.effects.test {
                viewModel.onIntent(LoginIntent.SubmitClicked)

                assertEquals(LoginEffect.FocusField(LoginField.Password), awaitItem())
            }
            val state = viewModel.state.value
            assertTrue(state.areCredentialsRejected)
            assertEquals(false, state.isSubmitting)
            assertEquals(VALID_EMAIL, state.email)
            assertEquals(VALID_PASSWORD, state.password)
        }

    @Test
    fun given_credentialsRejected_whenEmailEdited_thenClears() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.InvalidCredentials))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))
            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.state.value.areCredentialsRejected)

            viewModel.onIntent(LoginIntent.EmailChanged("$VALID_EMAIL.edited"))

            assertEquals(false, viewModel.state.value.areCredentialsRejected)
        }

    @Test
    fun given_credentialsRejected_whenPasswordEdited_thenClears() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.InvalidCredentials))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))
            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.state.value.areCredentialsRejected)

            viewModel.onIntent(LoginIntent.PasswordChanged("$VALID_PASSWORD-edited"))

            assertEquals(false, viewModel.state.value.areCredentialsRejected)
        }

    @Test
    fun given_emailTaken_whenSubmitted_thenShowsTakenErrorAndFocusesEmail() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.EmailTaken))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))

            viewModel.effects.test {
                viewModel.onIntent(LoginIntent.SubmitClicked)

                assertEquals(LoginEffect.FocusField(LoginField.Email), awaitItem())
            }
            assertEquals(EmailFieldError.Taken, viewModel.state.value.emailError)
        }

    @Test
    fun given_emailTakenError_whenEmailEdited_thenClears() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.EmailTaken))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))
            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(EmailFieldError.Taken, viewModel.state.value.emailError)

            viewModel.onIntent(LoginIntent.EmailChanged("$VALID_EMAIL.edited"))

            assertEquals(null, viewModel.state.value.emailError)
        }

    @Test
    fun given_noConnection_whenSubmitted_thenShowsOfflineBanner() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.NoConnection))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))

            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.isOfflineBannerVisible)
            assertEquals(false, state.isSubmitting)
        }

    @Test
    fun given_fieldsRejectedByServer_whenSubmitted_thenShowsMatchingErrorsAndFocusesFirst() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.FieldsRejected(setOf(AccountField.Password))))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))

            viewModel.effects.test {
                viewModel.onIntent(LoginIntent.SubmitClicked)

                assertEquals(LoginEffect.FocusField(LoginField.Password), awaitItem())
            }
            assertEquals(PasswordFieldError.TooShort(8), viewModel.state.value.passwordError)
        }

    @Test
    fun given_unexpectedError_whenSubmitted_thenEmitsShowUnexpectedErrorEffect() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.Unexpected))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))

            viewModel.effects.test {
                viewModel.onIntent(LoginIntent.SubmitClicked)

                assertEquals(LoginEffect.ShowUnexpectedError, awaitItem())
            }
            assertEquals(false, viewModel.state.value.isSubmitting)
        }

    @Test
    fun given_failedSubmission_whenSubmitClickedAgainLikeARetryTap_thenCallsTheRepositoryASecondTime() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(logInResult = DomainResult.Failure(AuthError.Unexpected))
            val viewModel = viewModel(repository)
            viewModel.onIntent(LoginIntent.EmailChanged(VALID_EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(VALID_PASSWORD))
            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            // The snackbar's Retry action and the field's IME Done action both just resend SubmitClicked.
            viewModel.onIntent(LoginIntent.SubmitClicked)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, repository.logInCalls.size)
        }
}
