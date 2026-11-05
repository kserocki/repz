package com.repz.app.core.data.user

import androidx.room3.Room
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

    override suspend fun getMe(): NetworkResult<UserDto> = getMeResult

    override suspend fun updateMe(request: UpdateProfileRequestDto): NetworkResult<UserDto> = NetworkResult.Failure(NetworkError.Offline)
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
            val repository = OfflineFirstUserRepository(FakeUserApi(), currentUserDao)

            assertNull(repository.observeCurrentUser().first())
        }

    @Test
    fun givenASuccessfulRefresh_whenRefreshing_thenTheRowIsStoredAndTheDomainUserIsReturned() =
        runTest {
            val userApi = FakeUserApi().apply { getMeResult = NetworkResult.Success(sampleUserDto()) }
            val repository = OfflineFirstUserRepository(userApi, currentUserDao)

            val result = repository.refreshCurrentUser()

            val success = assertIs<DomainResult.Success<User>>(result)
            assertEquals(User(UserId(sampleUserDto().id), "alex@example.com", "Alex", UnitSystem.KG), success.value)
            assertEquals(success.value, repository.observeCurrentUser().first())
        }

    @Test
    fun givenAStoredRow_whenRefreshFails_thenTheStoredRowStaysUnchanged() =
        runTest {
            val stored = sampleUserDto(displayName = "Alex")
            OfflineFirstUserRepository(FakeUserApi().apply { getMeResult = NetworkResult.Success(stored) }, currentUserDao)
                .refreshCurrentUser()
            val userApi = FakeUserApi().apply { getMeResult = NetworkResult.Failure(NetworkError.Offline) }
            val repository = OfflineFirstUserRepository(userApi, currentUserDao)

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

    private suspend fun refreshWith(error: NetworkError): DomainResult<User, UserRefreshError> {
        val userApi = FakeUserApi().apply { getMeResult = NetworkResult.Failure(error) }
        return OfflineFirstUserRepository(userApi, currentUserDao).refreshCurrentUser()
    }
}
