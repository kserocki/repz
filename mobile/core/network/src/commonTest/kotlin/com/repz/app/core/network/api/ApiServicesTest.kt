package com.repz.app.core.network.api

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.UnitSystemDto
import com.repz.app.core.network.dto.auth.LoginRequestDto
import com.repz.app.core.network.dto.auth.RegisterRequestDto
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.PushRequestDto
import com.repz.app.core.network.dto.user.UpdateProfileRequestDto
import com.repz.app.core.network.session.AccessToken
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.RefreshToken
import com.repz.app.core.network.testing.testClients
import com.repz.app.core.network.testing.testSessionManager
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

private suspend fun HttpRequestData.bodyText(): String = body.toByteArray().decodeToString()

private fun sessionResponseJson(displayName: String = "A"): String =
    """{"user":{"id":"b6e6b6a0-0000-4a22-8a33-000000000001","email":"a@b.com","displayName":"$displayName",""" +
        """"unitSystem":"kg"},"accessToken":"a","refreshToken":"r","expiresIn":900}"""

private fun emptyPullResponseJson(): String =
    """{"changes":[],"nextCursor":0,"hasMore":false,""" +
        """"totalEstimate":{"total":0,"workout":0,"template":0,"exercise":0}}"""

class ApiServicesTest {
    @Test
    fun givenRegister_whenCalled_thenItPostsToAuthRegisterOverThePublicClientWithTheBody() =
        runTest {
            var captured: HttpRequestData? = null
            val clients =
                testClients { request ->
                    captured = request
                    respond(sessionResponseJson(), HttpStatusCode.Created, jsonHeaders())
                }
            val authApi = KtorAuthApi(clients)

            authApi.register(RegisterRequestDto("a@b.com", "pw", "Alex"))

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/auth/register", request.url.encodedPath)
            assertNull(request.headers[HttpHeaders.Authorization])
            assertTrue(request.bodyText().contains("\"email\":\"a@b.com\""))
            assertTrue(request.bodyText().contains("\"displayName\":\"Alex\""))
        }

    @Test
    fun givenLogin_whenCalled_thenItPostsToAuthLoginOverThePublicClientWithTheBody() =
        runTest {
            var captured: HttpRequestData? = null
            val clients =
                testClients { request ->
                    captured = request
                    respond(sessionResponseJson(), HttpStatusCode.OK, jsonHeaders())
                }
            val authApi = KtorAuthApi(clients)

            authApi.login(LoginRequestDto("a@b.com", "pw"))

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/auth/login", request.url.encodedPath)
            assertTrue(request.bodyText().contains("\"email\":\"a@b.com\""))
        }

    @Test
    fun givenLogout_whenCalled_thenItPostsToAuthLogoutWithTheRefreshTokenInTheBody() =
        runTest {
            var captured: HttpRequestData? = null
            val clients =
                testClients { request ->
                    captured = request
                    respond("", HttpStatusCode.NoContent)
                }
            val authApi = KtorAuthApi(clients)

            authApi.logout(RefreshToken("the-refresh-token"))

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/auth/logout", request.url.encodedPath)
            assertTrue(request.bodyText().contains("the-refresh-token"))
        }

    @Test
    fun givenHealthCheck_whenCalled_thenItGetsHealthOverThePublicClient() =
        runTest {
            var captured: HttpRequestData? = null
            val clients =
                testClients { request ->
                    captured = request
                    respond("""{"status":"ok"}""", HttpStatusCode.OK, jsonHeaders())
                }
            val healthApi = KtorHealthApi(clients)

            healthApi.check()

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/health", request.url.encodedPath)
            assertNull(request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun givenGetMe_whenCalled_thenItGetsMeOverTheAuthenticatedClient() =
        runTest {
            var captured: HttpRequestData? = null
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken("acc"), RefreshToken("ref")))
            val clients =
                testClients(sessionManager = sessionManager) { request ->
                    captured = request
                    respond(
                        """{"id":"b6e6b6a0-0000-4a22-8a33-000000000001","email":"a@b.com","displayName":"A","unitSystem":"kg"}""",
                        HttpStatusCode.OK,
                        jsonHeaders(),
                    )
                }
            val userApi = KtorUserApi(clients)

            userApi.getMe()

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/me", request.url.encodedPath)
            assertEquals("Bearer acc", request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun givenUpdateMe_whenCalled_thenItPatchesMeOverTheAuthenticatedClientWithTheBody() =
        runTest {
            var captured: HttpRequestData? = null
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken("acc"), RefreshToken("ref")))
            val clients =
                testClients(sessionManager = sessionManager) { request ->
                    captured = request
                    respond(
                        """{"id":"b6e6b6a0-0000-4a22-8a33-000000000001","email":"a@b.com","displayName":"New Name","unitSystem":"kg"}""",
                        HttpStatusCode.OK,
                        jsonHeaders(),
                    )
                }
            val userApi = KtorUserApi(clients)

            userApi.updateMe(UpdateProfileRequestDto(displayName = "New Name"))

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals("/me", request.url.encodedPath)
            assertEquals("Bearer acc", request.headers[HttpHeaders.Authorization])
            assertTrue(request.bodyText().contains("\"displayName\":\"New Name\""))
            assertTrue(!request.bodyText().contains("unitSystem"))
        }

    @Test
    fun givenPush_whenCalled_thenItPostsToSyncPushOverTheAuthenticatedClientWithTheBody() =
        runTest {
            var captured: HttpRequestData? = null
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken("acc"), RefreshToken("ref")))
            val clients =
                testClients(sessionManager = sessionManager) { request ->
                    captured = request
                    respond("""{"accepted":[],"rejected":[]}""", HttpStatusCode.OK, jsonHeaders())
                }
            val syncApi = KtorSyncApi(clients)
            val changeId = Uuid.parse("b6e6b6a0-0000-4a22-8a33-000000000002")

            syncApi.push(
                PushRequestDto(
                    listOf(
                        PushChangeDto.UpsertCustomExercise(
                            changeId,
                            CustomExercisePayloadDto(
                                "Zottman curl",
                                com.repz.app.core.network.dto.MuscleDto.BICEPS,
                                emptyList(),
                                com.repz.app.core.network.dto.EquipmentDto.DUMBBELL,
                            ),
                        ),
                    ),
                ),
            )

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/sync/push", request.url.encodedPath)
            assertEquals("Bearer acc", request.headers[HttpHeaders.Authorization])
            assertTrue(request.bodyText().contains("\"type\":\"exercise\""))
        }

    @Test
    fun givenPull_whenCalled_thenItGetsSyncPullWithCursorAndLimitAsQueryParameters() =
        runTest {
            var captured: HttpRequestData? = null
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken("acc"), RefreshToken("ref")))
            val clients =
                testClients(sessionManager = sessionManager) { request ->
                    captured = request
                    respond(emptyPullResponseJson(), HttpStatusCode.OK, jsonHeaders())
                }
            val syncApi = KtorSyncApi(clients)

            syncApi.pull(cursor = 42, limit = 500)

            val request = requireNotNull(captured)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/sync/pull", request.url.encodedPath)
            assertEquals("42", request.url.parameters["cursor"])
            assertEquals("500", request.url.parameters["limit"])
            assertEquals("Bearer acc", request.headers[HttpHeaders.Authorization])
        }

    @Test
    fun givenANegativeCursor_whenPullIsCalled_thenItThrows() =
        runTest {
            val syncApi = KtorSyncApi(testClients { respond("{}", HttpStatusCode.OK) })

            assertFailsWith<IllegalArgumentException> { syncApi.pull(cursor = -1) }
        }

    @Test
    fun givenALimitOverFiveHundred_whenPullIsCalled_thenItThrows() =
        runTest {
            val syncApi = KtorSyncApi(testClients { respond("{}", HttpStatusCode.OK) })

            assertFailsWith<IllegalArgumentException> { syncApi.pull(cursor = 0, limit = 501) }
        }

    @Test
    fun givenAnEmptyChangesList_whenPushRequestDtoIsConstructed_thenItThrows() {
        assertFailsWith<IllegalArgumentException> { PushRequestDto(emptyList()) }
    }

    @Test
    fun givenMoreThanAHundredChanges_whenPushRequestDtoIsConstructed_thenItThrows() {
        val change = PushChangeDto.Delete(SyncTypeDto.WORKOUT, Uuid.parse("b6e6b6a0-0000-4a22-8a33-000000000003"))
        assertFailsWith<IllegalArgumentException> { PushRequestDto(List(101) { change }) }
    }

    @Test
    fun givenBothFieldsAbsent_whenUpdateProfileRequestDtoIsConstructed_thenItThrows() {
        assertFailsWith<IllegalArgumentException> { UpdateProfileRequestDto(displayName = null, unitSystem = null) }
    }

    @Test
    fun givenOnlyUnitSystem_whenUpdateProfileRequestDtoIsConstructed_thenItSucceeds() {
        val dto = UpdateProfileRequestDto(unitSystem = UnitSystemDto.LB)
        assertEquals(UnitSystemDto.LB, dto.unitSystem)
    }
}
