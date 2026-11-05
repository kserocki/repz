package com.repz.app.core.data.user

import androidx.room3.Room
import co.touchlab.kermit.Logger
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.user.UserRefreshError
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.dto.UnitSystemDto
import com.repz.app.core.network.dto.user.UpdateProfileRequestDto
import com.repz.app.core.network.dto.user.UserDto
import com.repz.app.core.network.result.NetworkError
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.session.AccessToken
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.RefreshToken
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.TokenSaveResult
import com.repz.app.core.network.session.TokenStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.uuid.Uuid

/** An API fake, not a domain repository fake (Konsist A5): `UserApi` belongs to `core:network`. */
private class FakeUserApi : UserApi {
    var getMeResult: NetworkResult<UserDto> = NetworkResult.Failure(NetworkError.Offline)

    /**
     * [reachedGetMeGate] completes right before suspending, so a test can wait for that instead of
     * assuming [getMe] is reached eagerly, then [getMeGate] holds the response until the test lets it through.
     */
    var getMeGate: CompletableDeferred<Unit>? = null
    var reachedGetMeGate: CompletableDeferred<Unit>? = null

    override suspend fun getMe(): NetworkResult<UserDto> {
        reachedGetMeGate?.complete(Unit)
        getMeGate?.await()
        return getMeResult
    }

    override suspend fun updateMe(request: UpdateProfileRequestDto): NetworkResult<UserDto> = NetworkResult.Failure(NetworkError.Offline)
}

/** An in-memory [TokenStore], just enough to drive [SessionManager] in these tests. */
private class FakeTokenStore : TokenStore {
    private var stored: AuthTokens? = null

    override suspend fun load(): AuthTokens? = stored

    override suspend fun save(tokens: AuthTokens): TokenSaveResult {
        stored = tokens
        return TokenSaveResult.Saved
    }

    override suspend fun clear() {
        stored = null
    }
}

private fun testLogger(): Logger = Logger.withTag("OfflineFirstUserRepositoryTest")

private suspend fun signedInSessionManager(refreshToken: String = "refresh"): SessionManager =
    SessionManager(FakeTokenStore(), testLogger()).apply {
        start(AuthTokens(AccessToken("access"), RefreshToken(refreshToken)))
    }

private fun sampleUserDto(
    id: Uuid = Uuid.parse("b6e6b6a0-1111-4a22-8a33-000000000001"),
    email: String = "alex@example.com",
    displayName: String = "Alex",
) = UserDto(id = id, email = email, displayName = displayName, unitSystem = UnitSystemDto.KG)

class OfflineFirstUserRepositoryTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database = Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.Default)
    private val currentUserDao = database.currentUserDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenNoStoredRow_whenObserved_thenEmitsNull() =
        runTest {
            val repository = OfflineFirstUserRepository(FakeUserApi(), currentUserDao, signedInSessionManager())

            assertNull(repository.observeCurrentUser().first())
        }

    @Test
    fun givenASuccessfulRefresh_whenRefreshing_thenTheRowIsStoredAndTheDomainUserIsReturned() =
        runTest {
            val userApi = FakeUserApi().apply { getMeResult = NetworkResult.Success(sampleUserDto()) }
            val repository = OfflineFirstUserRepository(userApi, currentUserDao, signedInSessionManager())

            val result = repository.refreshCurrentUser()

            val success = assertIs<DomainResult.Success<User>>(result)
            assertEquals(User(UserId(sampleUserDto().id), "alex@example.com", "Alex", UnitSystem.KG), success.value)
            assertEquals(success.value, repository.observeCurrentUser().first())
        }

    @Test
    fun givenAStoredRow_whenRefreshFails_thenTheStoredRowStaysUnchanged() =
        runTest {
            val stored = sampleUserDto(displayName = "Alex")
            val storingUserApi = FakeUserApi().apply { getMeResult = NetworkResult.Success(stored) }
            OfflineFirstUserRepository(storingUserApi, currentUserDao, signedInSessionManager()).refreshCurrentUser()
            val userApi = FakeUserApi().apply { getMeResult = NetworkResult.Failure(NetworkError.Offline) }
            val repository = OfflineFirstUserRepository(userApi, currentUserDao, signedInSessionManager())

            val result = repository.refreshCurrentUser()

            assertEquals(DomainResult.Failure(UserRefreshError.NoConnection), result)
            assertEquals("Alex", repository.observeCurrentUser().first()?.displayName)
        }

    @Test
    fun givenOffline_whenRefreshing_thenFailsWithNoConnection() =
        runTest {
            assertEquals(DomainResult.Failure(UserRefreshError.NoConnection), refreshWith(NetworkError.Offline))
        }

    @Test
    fun givenSessionExpired_whenRefreshing_thenFailsWithSessionExpired() =
        runTest {
            assertEquals(DomainResult.Failure(UserRefreshError.SessionExpired), refreshWith(NetworkError.SessionExpired))
        }

    @Test
    fun givenATimeout_whenRefreshing_thenFailsWithUnexpected() =
        runTest {
            assertEquals(DomainResult.Failure(UserRefreshError.Unexpected), refreshWith(NetworkError.Timeout))
        }

    @Test
    fun givenLogoutWhileARefreshIsInFlight_whenTheStaleResponseArrives_thenTheClearedRowIsNotReinserted() =
        runTest {
            val sessionManager = signedInSessionManager()
            currentUserDao.replace(sampleUserDto(displayName = "Alex").toCurrentUserEntity())
            val userApi =
                FakeUserApi().apply {
                    reachedGetMeGate = CompletableDeferred()
                    getMeGate = CompletableDeferred()
                    getMeResult = NetworkResult.Success(sampleUserDto(displayName = "Stale Alex"))
                }
            val repository = OfflineFirstUserRepository(userApi, currentUserDao, sessionManager)

            val refresh = launch { repository.refreshCurrentUser() }
            userApi.reachedGetMeGate?.await()
            // The session ends (as OfflineFirstAuthRepository.logOut does) while the request is still in flight.
            sessionManager.end()
            currentUserDao.clear()
            userApi.getMeGate?.complete(Unit)
            refresh.join()

            assertNull(currentUserDao.observe().first())
        }

    @Test
    fun givenADifferentAccountSignsInWhileARefreshIsInFlight_whenTheStaleResponseArrives_thenTheNewAccountsRowIsNotOverwritten() =
        runTest {
            val sessionManager = signedInSessionManager(refreshToken = "refresh-alex")
            val alexId = Uuid.parse("b6e6b6a0-1111-4a22-8a33-000000000001")
            currentUserDao.replace(sampleUserDto(id = alexId, displayName = "Alex").toCurrentUserEntity())
            val userApi =
                FakeUserApi().apply {
                    reachedGetMeGate = CompletableDeferred()
                    getMeGate = CompletableDeferred()
                    getMeResult = NetworkResult.Success(sampleUserDto(id = alexId, displayName = "Stale Alex"))
                }
            val repository = OfflineFirstUserRepository(userApi, currentUserDao, sessionManager)

            val refresh = launch { repository.refreshCurrentUser() }
            userApi.reachedGetMeGate?.await()
            // Alex logs out and Bailey signs in before Alex's stale GET /me response arrives.
            sessionManager.end()
            currentUserDao.clear()
            val baileyId = Uuid.parse("b6e6b6a0-1111-4a22-8a33-000000000002")
            currentUserDao.replace(sampleUserDto(id = baileyId, email = "bailey@example.com", displayName = "Bailey").toCurrentUserEntity())
            sessionManager.start(AuthTokens(AccessToken("access-bailey"), RefreshToken("refresh-bailey")))
            userApi.getMeGate?.complete(Unit)
            refresh.join()

            val storedUser = currentUserDao.observe().first()?.toDomain()
            assertEquals("Bailey", storedUser?.displayName)
        }

    private suspend fun refreshWith(error: NetworkError): DomainResult<User, UserRefreshError> {
        val userApi = FakeUserApi().apply { getMeResult = NetworkResult.Failure(error) }
        return OfflineFirstUserRepository(userApi, currentUserDao, signedInSessionManager()).refreshCurrentUser()
    }
}
