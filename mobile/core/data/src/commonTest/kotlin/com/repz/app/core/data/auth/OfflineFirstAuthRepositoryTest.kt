package com.repz.app.core.data.auth

import co.touchlab.kermit.Logger
import com.repz.app.core.domain.auth.AccountField
import com.repz.app.core.domain.auth.AuthError
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.domain.model.account.DisplayName
import com.repz.app.core.domain.model.account.EmailAddress
import com.repz.app.core.domain.model.account.Password
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.network.api.AuthApi
import com.repz.app.core.network.dto.UnitSystemDto
import com.repz.app.core.network.dto.auth.AuthSessionDto
import com.repz.app.core.network.dto.auth.LoginRequestDto
import com.repz.app.core.network.dto.auth.RegisterRequestDto
import com.repz.app.core.network.dto.user.UserDto
import com.repz.app.core.network.result.ApiProblem
import com.repz.app.core.network.result.NetworkError
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.ProblemFieldError
import com.repz.app.core.network.result.ProblemType
import com.repz.app.core.network.session.AccessToken
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.RefreshToken
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.SessionStatus
import com.repz.app.core.network.session.TokenSaveResult
import com.repz.app.core.network.session.TokenStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** An API fake, not a domain repository fake (Konsist A5): `AuthApi` belongs to `core:network`. */
private class FakeAuthApi : AuthApi {
    var registerResult: NetworkResult<AuthSessionDto> = NetworkResult.Failure(NetworkError.Offline)
    var loginResult: NetworkResult<AuthSessionDto> = NetworkResult.Failure(NetworkError.Offline)

    var registerGate: CompletableDeferred<Unit>? = null
    var loginGate: CompletableDeferred<Unit>? = null

    val registerCalls = mutableListOf<RegisterRequestDto>()
    val loginCalls = mutableListOf<LoginRequestDto>()

    override suspend fun register(request: RegisterRequestDto): NetworkResult<AuthSessionDto> {
        registerCalls += request
        registerGate?.await()
        return registerResult
    }

    override suspend fun login(request: LoginRequestDto): NetworkResult<AuthSessionDto> {
        loginCalls += request
        loginGate?.await()
        return loginResult
    }

    override suspend fun logout(refreshToken: RefreshToken): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

/** Same reasoning as `OfflineFirstSessionRepositoryTest`'s: `TokenStore` belongs to `core:network`. */
private class FakeTokenStore : TokenStore {
    private var stored: AuthTokens? = null
    var saveCallCount: Int = 0
        private set
    var saveResult: TokenSaveResult = TokenSaveResult.Saved

    override suspend fun load(): AuthTokens? = stored

    override suspend fun save(tokens: AuthTokens): TokenSaveResult {
        saveCallCount++
        if (saveResult == TokenSaveResult.Saved) stored = tokens
        return saveResult
    }

    override suspend fun clear() {
        stored = null
    }
}

private fun testLogger(): Logger = Logger.withTag("OfflineFirstAuthRepositoryTest")

private fun sampleUserDto() =
    UserDto(
        id = Uuid.parse("b6e6b6a0-1111-4a22-8a33-000000000001"),
        email = "alex@example.com",
        displayName = "Alex",
        unitSystem = UnitSystemDto.KG,
    )

private fun sampleSessionDto() =
    AuthSessionDto(
        user = sampleUserDto(),
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 900,
    )

private fun sampleEmail() = (EmailAddress.of("alex@example.com") as DomainResult.Success).value

private fun samplePassword() = (Password.forLogIn("correct horse battery staple") as DomainResult.Success).value

private fun sampleDisplayName() = (DisplayName.of("Alex") as DomainResult.Success).value

private fun apiProblem(
    type: ProblemType,
    status: Int,
    fieldErrors: List<ProblemFieldError> = emptyList(),
) = ApiProblem(type = type, title = "problem", status = status, detail = null, requestId = "req-1", fieldErrors = fieldErrors)

class OfflineFirstAuthRepositoryTest {
    @Test
    fun givenASuccessfulLogin_whenLoggingIn_thenTheSessionStartsAndTheDomainUserIsReturned() =
        runTest {
            val authApi = FakeAuthApi().apply { loginResult = NetworkResult.Success(sampleSessionDto()) }
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = OfflineFirstAuthRepository(authApi, sessionManager)

            val result = repository.logIn(sampleEmail(), samplePassword())

            val success = assertIs<DomainResult.Success<User>>(result)
            assertEquals(User(UserId(sampleUserDto().id), "alex@example.com", "Alex", UnitSystem.KG), success.value)
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
            assertEquals(1, tokenStore.saveCallCount)
            assertEquals(AuthTokens(AccessToken("access-token"), RefreshToken("refresh-token")), tokenStore.load())
        }

    @Test
    fun givenASuccessfulRegister_whenRegistering_thenTheSessionStartsAndTheDomainUserIsReturned() =
        runTest {
            val authApi = FakeAuthApi().apply { registerResult = NetworkResult.Success(sampleSessionDto()) }
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = OfflineFirstAuthRepository(authApi, sessionManager)

            val result = repository.register(sampleEmail(), samplePassword(), sampleDisplayName())

            val success = assertIs<DomainResult.Success<User>>(result)
            assertEquals("Alex", success.value.displayName)
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
            assertEquals(1, tokenStore.saveCallCount)
            assertEquals(1, authApi.registerCalls.size)
        }

    @Test
    fun givenATokenSaveFailure_whenLoggingIn_thenItStillSucceedsAndSignsInForThisProcess() =
        runTest {
            val authApi = FakeAuthApi().apply { loginResult = NetworkResult.Success(sampleSessionDto()) }
            val tokenStore = FakeTokenStore().apply { saveResult = TokenSaveResult.Failed }
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = OfflineFirstAuthRepository(authApi, sessionManager)

            val result = repository.logIn(sampleEmail(), samplePassword())

            assertIs<DomainResult.Success<User>>(result)
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
        }

    @Test
    fun given401WithInvalidCredentials_whenLoggingIn_thenFailsWithInvalidCredentials() =
        runTest {
            val error = NetworkError.Api(401, apiProblem(ProblemType.InvalidCredentials, 401))
            val result = logInWith(NetworkResult.Failure(error))

            assertEquals(DomainResult.Failure(AuthError.InvalidCredentials), result)
        }

    @Test
    fun given409WithEmailTaken_whenRegistering_thenFailsWithEmailTaken() =
        runTest {
            val error = NetworkError.Api(409, apiProblem(ProblemType.EmailTaken, 409))
            val result = registerWith(NetworkResult.Failure(error))

            assertEquals(DomainResult.Failure(AuthError.EmailTaken), result)
        }

    @Test
    fun given400ValidationErrorWithKnownPaths_whenRegistering_thenFailsWithFieldsRejected() =
        runTest {
            val fieldErrors =
                listOf(
                    ProblemFieldError("email", "Invalid email"),
                    ProblemFieldError("password", "Too short"),
                    ProblemFieldError("displayName", "Blank"),
                )
            val error = NetworkError.Api(400, apiProblem(ProblemType.ValidationError, 400, fieldErrors))
            val result = registerWith(NetworkResult.Failure(error))

            assertEquals(
                DomainResult.Failure(AuthError.FieldsRejected(setOf(AccountField.Email, AccountField.Password, AccountField.DisplayName))),
                result,
            )
        }

    @Test
    fun given400ValidationErrorWithNoKnownPath_whenRegistering_thenFailsWithUnexpected() =
        runTest {
            val error = NetworkError.Api(400, apiProblem(ProblemType.ValidationError, 400, listOf(ProblemFieldError("unknown", "?"))))
            val result = registerWith(NetworkResult.Failure(error))

            assertEquals(DomainResult.Failure(AuthError.Unexpected), result)
        }

    @Test
    fun givenOffline_whenLoggingIn_thenFailsWithNoConnection() =
        runTest {
            val result = logInWith(NetworkResult.Failure(NetworkError.Offline))

            assertEquals(DomainResult.Failure(AuthError.NoConnection), result)
        }

    @Test
    fun givenATimeout_whenLoggingIn_thenFailsWithUnexpected() =
        runTest {
            val result = logInWith(NetworkResult.Failure(NetworkError.Timeout))

            assertEquals(DomainResult.Failure(AuthError.Unexpected), result)
        }

    @Test
    fun givenAMalformedResponse_whenLoggingIn_thenFailsWithUnexpected() =
        runTest {
            val result = logInWith(NetworkResult.Failure(NetworkError.MalformedResponse("bad body")))

            assertEquals(DomainResult.Failure(AuthError.Unexpected), result)
        }

    @Test
    fun givenSessionExpired_whenLoggingIn_thenFailsWithUnexpected() =
        runTest {
            val result = logInWith(NetworkResult.Failure(NetworkError.SessionExpired))

            assertEquals(DomainResult.Failure(AuthError.Unexpected), result)
        }

    @Test
    fun givenAnUnrecognisedApiStatus_whenLoggingIn_thenFailsWithUnexpected() =
        runTest {
            val error = NetworkError.Api(500, apiProblem(ProblemType.PlainHttp, 500))
            val result = logInWith(NetworkResult.Failure(error))

            assertEquals(DomainResult.Failure(AuthError.Unexpected), result)
        }

    @Test
    fun givenAFailure_whenLoggingIn_thenNoSessionStartsAndNothingIsSaved() =
        runTest {
            val authApi = FakeAuthApi().apply { loginResult = NetworkResult.Failure(NetworkError.Offline) }
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = OfflineFirstAuthRepository(authApi, sessionManager)

            repository.logIn(sampleEmail(), samplePassword())

            assertEquals(SessionStatus.Unknown, sessionManager.status.value)
            assertEquals(0, tokenStore.saveCallCount)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun givenTheCoroutineIsCancelledWhileLoggingIn_whenCancelled_thenTheCancellationPropagatesAndNoSessionStarts() =
        runTest(UnconfinedTestDispatcher()) {
            val authApi = FakeAuthApi().apply { loginGate = CompletableDeferred() }
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = OfflineFirstAuthRepository(authApi, sessionManager)

            val job = launch { repository.logIn(sampleEmail(), samplePassword()) }
            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            assertEquals(1, authApi.loginCalls.size)
            assertEquals(SessionStatus.Unknown, sessionManager.status.value)
            assertEquals(0, tokenStore.saveCallCount)
        }

    private suspend fun logInWith(result: NetworkResult<AuthSessionDto>): DomainResult<User, AuthError> {
        val authApi = FakeAuthApi().apply { loginResult = result }
        val repository = OfflineFirstAuthRepository(authApi, SessionManager(FakeTokenStore(), testLogger()))
        return repository.logIn(sampleEmail(), samplePassword())
    }

    private suspend fun registerWith(result: NetworkResult<AuthSessionDto>): DomainResult<User, AuthError> {
        val authApi = FakeAuthApi().apply { registerResult = result }
        val repository = OfflineFirstAuthRepository(authApi, SessionManager(FakeTokenStore(), testLogger()))
        return repository.register(sampleEmail(), samplePassword(), sampleDisplayName())
    }
}
