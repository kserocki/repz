package com.repz.app.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import co.touchlab.kermit.Logger
import com.repz.app.core.data.newTestUserPreferencesDataSource
import com.repz.app.core.data.user.toCurrentUserEntity
import com.repz.app.core.data.user.toDomain
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.account.CurrentUserDao
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.datastore.UserPreferencesDataSource
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
import com.repz.app.core.network.session.SignOutReason
import com.repz.app.core.network.session.TokenSaveResult
import com.repz.app.core.network.session.TokenStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.IOException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** An API fake, not a domain repository fake (Konsist A5): `AuthApi` belongs to `core:network`. */
private class FakeAuthApi : AuthApi {
    var registerResult: NetworkResult<AuthSessionDto> = NetworkResult.Failure(NetworkError.Offline)
    var loginResult: NetworkResult<AuthSessionDto> = NetworkResult.Failure(NetworkError.Offline)
    var logoutResult: NetworkResult<Unit> = NetworkResult.Success(Unit)

    var registerGate: CompletableDeferred<Unit>? = null
    var loginGate: CompletableDeferred<Unit>? = null

    val registerCalls = mutableListOf<RegisterRequestDto>()
    val loginCalls = mutableListOf<LoginRequestDto>()
    val logoutCalls = mutableListOf<RefreshToken>()

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

    override suspend fun logout(refreshToken: RefreshToken): NetworkResult<Unit> {
        logoutCalls += refreshToken
        return logoutResult
    }
}

/** Same reasoning as `OfflineFirstSessionRepositoryTest`'s: `TokenStore` belongs to `core:network`. */
private class FakeTokenStore : TokenStore {
    private var stored: AuthTokens? = null
    var saveCallCount: Int = 0
        private set
    var saveResult: TokenSaveResult = TokenSaveResult.Saved

    /**
     * Suspends inside [SessionManager.end] so a test can cancel the caller while it holds this
     * lock. [reachedClearGate] completes right before suspending, so the test can wait for that
     * instead of assuming [clear] runs eagerly (it may hop through a real dispatcher first, e.g.
     * a prior Room call on the same coroutine).
     */
    var clearGate: CompletableDeferred<Unit>? = null
    var reachedClearGate: CompletableDeferred<Unit>? = null
    var clearCallCount: Int = 0
        private set

    override suspend fun load(): AuthTokens? = stored

    override suspend fun save(tokens: AuthTokens): TokenSaveResult {
        saveCallCount++
        if (saveResult == TokenSaveResult.Saved) stored = tokens
        return saveResult
    }

    override suspend fun clear() {
        clearCallCount++
        reachedClearGate?.complete(Unit)
        clearGate?.await()
        stored = null
    }
}

/** A [DataStore] whose write always fails, to exercise [OfflineFirstAuthRepository]'s defensive catch (Konsist E1). */
private class FailingPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flowOf(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("disk full")
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
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.Default)
    private val currentUserDao = database.currentUserDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun repository(
        authApi: AuthApi,
        sessionManager: SessionManager,
        dao: CurrentUserDao = currentUserDao,
        userPreferencesDataSource: UserPreferencesDataSource = newTestUserPreferencesDataSource(),
        applicationScope: CoroutineScope = TestScope(),
    ) = OfflineFirstAuthRepository(authApi, sessionManager, dao, userPreferencesDataSource, applicationScope, testLogger())

    @Test
    fun givenASuccessfulLogin_whenLoggingIn_thenTheSessionStartsAndTheDomainUserIsReturned() =
        runTest {
            val authApi = FakeAuthApi().apply { loginResult = NetworkResult.Success(sampleSessionDto()) }
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = repository(authApi, sessionManager)

            val result = repository.logIn(sampleEmail(), samplePassword())

            val success = assertIs<DomainResult.Success<User>>(result)
            assertEquals(User(UserId(sampleUserDto().id), "alex@example.com", "Alex", UnitSystem.KG), success.value)
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
            assertEquals(1, tokenStore.saveCallCount)
            assertEquals(AuthTokens(AccessToken("access-token"), RefreshToken("refresh-token")), tokenStore.load())
        }

    @Test
    fun givenASuccessfulLogin_whenLoggingIn_thenTheRowIsStoredBeforeTheSessionStarts() =
        runTest {
            val authApi = FakeAuthApi().apply { loginResult = NetworkResult.Success(sampleSessionDto()) }
            val sessionManager = SessionManager(FakeTokenStore(), testLogger())
            val repository = repository(authApi, sessionManager)

            repository.logIn(sampleEmail(), samplePassword())

            assertEquals(sampleUserDto().toDomain(), currentUserDao.observe().first()?.toDomain())
        }

    @Test
    fun givenASuccessfulRegister_whenRegistering_thenTheSessionStartsAndTheDomainUserIsReturned() =
        runTest {
            val authApi = FakeAuthApi().apply { registerResult = NetworkResult.Success(sampleSessionDto()) }
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = repository(authApi, sessionManager)

            val result = repository.register(sampleEmail(), samplePassword(), sampleDisplayName())

            val success = assertIs<DomainResult.Success<User>>(result)
            assertEquals("Alex", success.value.displayName)
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
            assertEquals(1, tokenStore.saveCallCount)
            assertEquals(1, authApi.registerCalls.size)
        }

    @Test
    fun givenASuccessfulRegister_whenRegistering_thenTheRowIsStoredBeforeTheSessionStarts() =
        runTest {
            val authApi = FakeAuthApi().apply { registerResult = NetworkResult.Success(sampleSessionDto()) }
            val sessionManager = SessionManager(FakeTokenStore(), testLogger())
            val repository = repository(authApi, sessionManager)

            repository.register(sampleEmail(), samplePassword(), sampleDisplayName())

            assertEquals(sampleUserDto().toDomain(), currentUserDao.observe().first()?.toDomain())
        }

    @Test
    fun givenATokenSaveFailure_whenLoggingIn_thenItStillSucceedsAndSignsInForThisProcess() =
        runTest {
            val authApi = FakeAuthApi().apply { loginResult = NetworkResult.Success(sampleSessionDto()) }
            val tokenStore = FakeTokenStore().apply { saveResult = TokenSaveResult.Failed }
            val sessionManager = SessionManager(tokenStore, testLogger())
            val repository = repository(authApi, sessionManager)

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
            // Real shape from contract/fixtures/register/400/validation-error.json: the hook joins
            // [result.target, ...issue.path] with ".", so every path is prefixed by its target ("json").
            val fieldErrors =
                listOf(
                    ProblemFieldError("json.email", "Invalid email address"),
                    ProblemFieldError("json.password", "Too small: expected string to have >=8 characters"),
                    ProblemFieldError("json.displayName", "Too small: expected string to have >=1 characters"),
                )
            val error = NetworkError.Api(400, apiProblem(ProblemType.ValidationError, 400, fieldErrors))
            val result = registerWith(NetworkResult.Failure(error))

            assertEquals(
                DomainResult.Failure(AuthError.FieldsRejected(setOf(AccountField.Email, AccountField.Password, AccountField.DisplayName))),
                result,
            )
        }

    @Test
    fun given400ValidationErrorWithTargetPrefixedPasswordPath_whenLoggingIn_thenFailsWithFieldsRejected() =
        runTest {
            // Real shape from contract/fixtures/login/400/validation-error.json.
            val fieldErrors = listOf(ProblemFieldError("json.password", "Too small: expected string to have >=1 characters"))
            val error = NetworkError.Api(400, apiProblem(ProblemType.ValidationError, 400, fieldErrors))
            val result = logInWith(NetworkResult.Failure(error))

            assertEquals(DomainResult.Failure(AuthError.FieldsRejected(setOf(AccountField.Password))), result)
        }

    @Test
    fun given400ValidationErrorWithNoKnownPath_whenRegistering_thenFailsWithUnexpected() =
        runTest {
            val error = NetworkError.Api(400, apiProblem(ProblemType.ValidationError, 400, listOf(ProblemFieldError("json.unknown", "?"))))
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
            val repository = repository(authApi, sessionManager)

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
            val repository = repository(authApi, sessionManager)

            val job = launch { repository.logIn(sampleEmail(), samplePassword()) }
            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            assertEquals(1, authApi.loginCalls.size)
            assertEquals(SessionStatus.Unknown, sessionManager.status.value)
            assertEquals(0, tokenStore.saveCallCount)
        }

    @Test
    fun givenASignedInSession_whenLoggingOut_thenTokensAndTheRowAreClearedAndSignOutReasonIsLoggedOut() =
        runTest {
            val tokenStore = FakeTokenStore().apply { saveResult = TokenSaveResult.Saved }
            val sessionManager = SessionManager(tokenStore, testLogger())
            sessionManager.start(AuthTokens(AccessToken("access"), RefreshToken("refresh")))
            currentUserDao.replace(sampleUserDto().toCurrentUserEntity())
            val repository = repository(FakeAuthApi(), sessionManager)

            repository.logOut()

            assertEquals(SessionStatus.SignedOut(SignOutReason.LoggedOut), sessionManager.status.value)
            assertNull(tokenStore.load())
            assertNull(currentUserDao.observe().first())
        }

    @Test
    fun givenAStoredDeviceUnitPreference_whenLoggingOut_thenItIsCleared() =
        runTest {
            val sessionManager = SessionManager(FakeTokenStore(), testLogger())
            sessionManager.start(AuthTokens(AccessToken("access"), RefreshToken("refresh")))
            val userPreferencesDataSource = newTestUserPreferencesDataSource()
            userPreferencesDataSource.setUnitSystem("lb")
            val repository = repository(FakeAuthApi(), sessionManager, userPreferencesDataSource = userPreferencesDataSource)

            repository.logOut()

            assertNull(userPreferencesDataSource.preferences.first().unitSystem)
        }

    @Test
    fun givenTheRevokeFailsOffline_whenLoggingOut_thenTheLocalStateIsStillCleared() =
        runTest {
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            sessionManager.start(AuthTokens(AccessToken("access"), RefreshToken("refresh")))
            val authApi = FakeAuthApi().apply { logoutResult = NetworkResult.Failure(NetworkError.Offline) }
            val repository = repository(authApi, sessionManager)

            repository.logOut()

            assertEquals(SessionStatus.SignedOut(SignOutReason.LoggedOut), sessionManager.status.value)
            assertNull(tokenStore.load())
        }

    @Test
    fun givenTheRevokeFailsWithServerError_whenLoggingOut_thenTheLocalStateIsStillCleared() =
        runTest {
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            sessionManager.start(AuthTokens(AccessToken("access"), RefreshToken("refresh")))
            val authApi =
                FakeAuthApi().apply {
                    logoutResult = NetworkResult.Failure(NetworkError.Api(500, apiProblem(ProblemType.PlainHttp, 500)))
                }
            val repository = repository(authApi, sessionManager)

            repository.logOut()

            assertEquals(SessionStatus.SignedOut(SignOutReason.LoggedOut), sessionManager.status.value)
            assertNull(tokenStore.load())
        }

    @Test
    fun givenASignedInSession_whenLoggingOut_thenTheRevokeIsCalledOnceWithTheCapturedToken() =
        runTest {
            val applicationScope = TestScope(testScheduler)
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            sessionManager.start(AuthTokens(AccessToken("access"), RefreshToken("the-refresh-token")))
            val authApi = FakeAuthApi()
            val repository = repository(authApi, sessionManager, applicationScope = applicationScope)

            repository.logOut()
            applicationScope.testScheduler.advanceUntilIdle()

            assertEquals(listOf(RefreshToken("the-refresh-token")), authApi.logoutCalls)
        }

    @Test
    fun givenTheDeviceUnitPreferenceClearFails_whenLoggingOut_thenTheLocalStateIsStillClearedAndTheRevokeStillRuns() =
        runTest {
            val applicationScope = TestScope(testScheduler)
            val tokenStore = FakeTokenStore()
            val sessionManager = SessionManager(tokenStore, testLogger())
            sessionManager.start(AuthTokens(AccessToken("access"), RefreshToken("the-refresh-token")))
            currentUserDao.replace(sampleUserDto().toCurrentUserEntity())
            val authApi = FakeAuthApi()
            val userPreferencesDataSource = UserPreferencesDataSource(FailingPreferencesDataStore())
            val repository =
                repository(
                    authApi,
                    sessionManager,
                    userPreferencesDataSource = userPreferencesDataSource,
                    applicationScope = applicationScope,
                )

            repository.logOut()
            applicationScope.testScheduler.advanceUntilIdle()

            assertEquals(SessionStatus.SignedOut(SignOutReason.LoggedOut), sessionManager.status.value)
            assertNull(tokenStore.load())
            assertNull(currentUserDao.observe().first())
            assertEquals(listOf(RefreshToken("the-refresh-token")), authApi.logoutCalls)
        }

    @Test
    fun givenAnAlreadySignedOutSession_whenLoggingOut_thenNoRevokeIsCalled() =
        runTest {
            val applicationScope = TestScope(testScheduler)
            val sessionManager = SessionManager(FakeTokenStore(), testLogger())
            val authApi = FakeAuthApi()
            val repository = repository(authApi, sessionManager, applicationScope = applicationScope)

            repository.logOut()
            applicationScope.testScheduler.advanceUntilIdle()

            assertTrue(authApi.logoutCalls.isEmpty())
        }

    @Test
    fun givenTheCallingCoroutineIsCancelledDuringTheLocalCleanup_whenLoggingOut_thenTheLocalStepsStillFinishAndTheRevokeSurvives() =
        runTest {
            val tokenStore =
                FakeTokenStore().apply {
                    reachedClearGate = CompletableDeferred()
                    clearGate = CompletableDeferred()
                }
            val sessionManager = SessionManager(tokenStore, testLogger())
            sessionManager.start(AuthTokens(AccessToken("access"), RefreshToken("refresh")))
            currentUserDao.replace(sampleUserDto().toCurrentUserEntity())
            val authApi = FakeAuthApi()
            // A scope that outlives the cancelled caller below, the same as applicationScope in production.
            val applicationScope = TestScope(testScheduler)
            val repository = repository(authApi, sessionManager, applicationScope = applicationScope)

            val job = launch { repository.logOut() }
            tokenStore.reachedClearGate?.await()
            job.cancel()
            assertEquals(1, tokenStore.clearCallCount)
            tokenStore.clearGate?.complete(Unit)
            job.join()
            applicationScope.testScheduler.advanceUntilIdle()

            assertTrue(job.isCancelled)
            assertEquals(SessionStatus.SignedOut(SignOutReason.LoggedOut), sessionManager.status.value)
            assertNull(tokenStore.load())
            assertNull(currentUserDao.observe().first())
            assertEquals(listOf(RefreshToken("refresh")), authApi.logoutCalls)
        }

    private suspend fun logInWith(result: NetworkResult<AuthSessionDto>): DomainResult<User, AuthError> {
        val authApi = FakeAuthApi().apply { loginResult = result }
        return repository(authApi, SessionManager(FakeTokenStore(), testLogger())).logIn(sampleEmail(), samplePassword())
    }

    private suspend fun registerWith(result: NetworkResult<AuthSessionDto>): DomainResult<User, AuthError> {
        val authApi = FakeAuthApi().apply { registerResult = result }
        return repository(authApi, SessionManager(FakeTokenStore(), testLogger()))
            .register(sampleEmail(), samplePassword(), sampleDisplayName())
    }
}
