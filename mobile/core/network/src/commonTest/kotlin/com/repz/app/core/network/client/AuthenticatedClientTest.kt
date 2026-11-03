package com.repz.app.core.network.client

import com.repz.app.core.network.api.KtorUserApi
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.result.NetworkError
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.result.apiCall
import com.repz.app.core.network.session.AccessToken
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.PersistentTokenStore
import com.repz.app.core.network.session.RefreshToken
import com.repz.app.core.network.session.SessionStatus
import com.repz.app.core.network.session.SignOutReason
import com.repz.app.core.network.testing.InMemorySecureStringStorage
import com.repz.app.core.network.testing.testClients
import com.repz.app.core.network.testing.testSessionManager
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// What the fake server currently considers valid.
private const val INITIAL_ACCESS_TOKEN = "access-0"
private const val INITIAL_REFRESH_TOKEN = "refresh-0"

// What the client starts with: deliberately not INITIAL_ACCESS_TOKEN, so the first authenticated
// call always gets a 401 and has to refresh, the same as a real access token that expired server-side.
private const val STALE_ACCESS_TOKEN = "stale-access"

private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

private fun problemJsonHeaders(vararg extra: Pair<String, String>) =
    headersOf(
        HttpHeaders.ContentType to listOf("application/problem+json"),
        *extra.map { it.first to listOf(it.second) }.toTypedArray(),
    )

private fun problemBody(
    urn: String,
    title: String,
    status: Int,
): String = """{"type":"urn:repz:problem:$urn","title":"$title","status":$status,"instance":"/auth/refresh","requestId":"r"}"""

/** A minimal fake auth server: rotates tokens on refresh, requires the current access token for `/me` and the sync routes. */
private class RotatingAuthServer {
    private var accessToken = INITIAL_ACCESS_TOKEN
    private var refreshToken = INITIAL_REFRESH_TOKEN

    var refreshCallCount = 0
        private set

    /** Overrides the refresh response entirely (e.g. a 401, a 503, or a thrown transport exception). */
    var refreshBehavior: (suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData)? = null

    suspend fun handle(
        scope: MockRequestHandleScope,
        request: HttpRequestData,
    ): HttpResponseData =
        when (request.url.encodedPath) {
            "/auth/refresh" -> {
                refreshCallCount++
                val override = refreshBehavior
                if (override != null) {
                    override(scope, request)
                } else {
                    accessToken = "access-$refreshCallCount"
                    refreshToken = "refresh-$refreshCallCount"
                    scope.respond(
                        """{"accessToken":"$accessToken","refreshToken":"$refreshToken","expiresIn":900}""",
                        HttpStatusCode.OK,
                        jsonHeaders(),
                    )
                }
            }

            "/auth/login" -> {
                scope.respond(
                    """{"type":"urn:repz:problem:invalid-credentials","title":"x","status":401,"instance":"/auth/login","requestId":"r"}""",
                    HttpStatusCode.Unauthorized,
                    problemJsonHeaders(),
                )
            }

            else -> {
                val header = request.headers[HttpHeaders.Authorization]
                if (header == "Bearer $accessToken") {
                    scope.respond("""{"ok":true}""", HttpStatusCode.OK, jsonHeaders())
                } else {
                    scope.respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
                }
            }
        }

    fun currentTokens() = AuthTokens(AccessToken(accessToken), RefreshToken(refreshToken))
}

class AuthenticatedClientTest {
    @Test
    fun givenTheAuthenticatedClient_whenCallingMeOrSync_thenBearerIsAttached_andNeverOnPublicRoutes() =
        runTest {
            val headersSeen = mutableMapOf<String, String?>()
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken("acc"), RefreshToken("ref")))
            val clients =
                testClients(sessionManager = sessionManager) { request ->
                    headersSeen[request.url.encodedPath] = request.headers[HttpHeaders.Authorization]
                    respond("{}", HttpStatusCode.OK, jsonHeaders())
                }

            clients.authenticated.get("/me")
            clients.authenticated.get("/sync/pull")
            clients.public.get("/health")

            assertEquals("Bearer acc", headersSeen["/me"])
            assertEquals("Bearer acc", headersSeen["/sync/pull"])
            assertNull(headersSeen["/health"])
        }

    @Test
    fun givenAn401_whenTheAuthenticatedClientCallsMe_thenItRefreshesOnceAndRetriesSuccessfully() =
        runTest {
            val server = RotatingAuthServer()
            val storage = InMemorySecureStringStorage()
            val sessionManager = testSessionManager(storage)
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            val response = clients.authenticated.get("/me")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, server.refreshCallCount)
            assertEquals(server.currentTokens(), sessionManager.currentTokens())
            // The new tokens are persisted, not just cached in memory for this process.
            assertEquals(server.currentTokens(), PersistentTokenStore(storage).load())
        }

    @Test
    fun givenFiveConcurrent401s_whenTheyAllRefresh_thenExactlyOneRefreshCallIsMadeAndAllFiveSucceed() =
        runTest {
            val server = RotatingAuthServer()
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            val results = (1..5).map { async { clients.authenticated.get("/sync/pull") } }.awaitAll()

            assertTrue(results.all { it.status == HttpStatusCode.OK })
            assertEquals(1, server.refreshCallCount)
        }

    @Test
    fun givenRefreshReturns401_whenTheAuthenticatedClientCallsMe_thenSessionExpiresAndStoreIsCleared() =
        runTest {
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior = { _ ->
                        respond(
                            problemBody("invalid-refresh-token", "x", 401),
                            HttpStatusCode.Unauthorized,
                            problemJsonHeaders(),
                        )
                    }
                }
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            clients.authenticated.get("/me")

            val status = sessionManager.status.value
            assertIs<SessionStatus.SignedOut>(status)
            assertEquals(SignOutReason.Expired, status.reason)
            assertNull(sessionManager.currentTokens())
        }

    @Test
    fun givenRefreshReturns400_whenTheAuthenticatedClientCallsMe_thenSessionExpiresToo() =
        runTest {
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior =
                        { _ ->
                            respond(
                                problemBody("validation-error", "x", 400),
                                HttpStatusCode.BadRequest,
                                problemJsonHeaders(),
                            )
                        }
                }
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            clients.authenticated.get("/me")

            val status = sessionManager.status.value
            assertIs<SessionStatus.SignedOut>(status)
            assertEquals(SignOutReason.Expired, status.reason)
        }

    /**
     * The contract [NetworkError.SessionExpired] (`result/ApiCall.kt`'s `hasJustExpired`) depends on,
     * exercised through a real [UserApi] call rather than a bare Ktor response, the same way a
     * real caller higher up the stack would see it.
     */
    @Test
    fun givenRefreshReturns401_whenGetMeIsCalled_thenTheResultIsSessionExpired() =
        runTest {
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior = { _ ->
                        respond(
                            problemBody("invalid-refresh-token", "x", 401),
                            HttpStatusCode.Unauthorized,
                            problemJsonHeaders(),
                        )
                    }
                }
            val storage = InMemorySecureStringStorage()
            val sessionManager = testSessionManager(storage)
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }
            val userApi: UserApi = KtorUserApi(clients)

            val result = userApi.getMe()

            assertEquals(NetworkResult.Failure(NetworkError.SessionExpired), result)
            val status = sessionManager.status.value
            assertIs<SessionStatus.SignedOut>(status)
            assertEquals(SignOutReason.Expired, status.reason)
            assertNull(storage.read())
        }

    /**
     * A network/5xx refresh failure throws [com.repz.app.core.network.session.RefreshUnavailableException]
     * out of the whole bearer + refresh pipeline (Ktor's own code has no catch around it), so this
     * goes through [apiCall] like every real caller, not a bare Ktor client call: that's the one
     * place it's turned into the refresh's own [NetworkError], not the original 401.
     */
    @Test
    fun givenRefreshReturns503_whenTheAuthenticatedClientCallsMe_thenTheRefreshFailureSurfacesAndTokensStayUnchanged() =
        runTest {
            val originalTokens = AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN))
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior = { _ -> respond("service unavailable", HttpStatusCode.ServiceUnavailable) }
                }
            val sessionManager = testSessionManager()
            sessionManager.start(originalTokens)
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            val result =
                apiCall(
                    sessionManager = sessionManager,
                    decode = { it.body<String>() },
                    request = { clients.authenticated.get("/me") },
                )

            assertEquals(NetworkResult.Failure(NetworkError.Api(503, null)), result)
            assertEquals(originalTokens, sessionManager.currentTokens())
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
        }

    @Test
    fun givenRefreshFailsOffline_whenTheAuthenticatedClientCallsMe_thenTheRefreshFailureSurfacesAndTokensStayUnchanged() =
        runTest {
            val originalTokens = AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN))
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior = { _ -> throw IOException("no route to the server") }
                }
            val sessionManager = testSessionManager()
            sessionManager.start(originalTokens)
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            val result =
                apiCall(
                    sessionManager = sessionManager,
                    decode = { it.body<String>() },
                    request = { clients.authenticated.get("/me") },
                )

            assertEquals(NetworkResult.Failure(NetworkError.Offline), result)
            assertEquals(originalTokens, sessionManager.currentTokens())
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
        }

    @Test
    fun givenRefreshTimesOut_whenTheAuthenticatedClientCallsMe_thenTheRefreshFailureSurfacesAndTokensStayUnchanged() =
        runTest {
            val originalTokens = AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN))
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior = { request -> throw HttpRequestTimeoutException(url = request.url.toString(), timeoutMillis = 1000) }
                }
            val sessionManager = testSessionManager()
            sessionManager.start(originalTokens)
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            val result =
                apiCall(
                    sessionManager = sessionManager,
                    decode = { it.body<String>() },
                    request = { clients.authenticated.get("/me") },
                )

            assertEquals(NetworkResult.Failure(NetworkError.Timeout), result)
            assertEquals(originalTokens, sessionManager.currentTokens())
            assertEquals(SessionStatus.SignedIn, sessionManager.status.value)
        }

    @Test
    fun givenLoginReturns401_whenCalledOverThePublicClient_thenNoRefreshIsAttempted() =
        runTest {
            val server = RotatingAuthServer()
            val clients = testClients { request -> server.handle(this, request) }

            clients.public.post("/auth/login")

            assertEquals(0, server.refreshCallCount)
        }

    /**
     * Verified against the `ktor-client-auth` 3.6.0 source (`Auth.kt`'s `findProvider`): with
     * exactly one bearer provider installed (our setup, always), the `candidateProviders.size == 1`
     * branch is taken unconditionally and a refresh is attempted regardless of whether the 401
     * carried a `WWW-Authenticate` header. The header only affects which challenge is passed to
     * `addRequestHeaders` on the retry, not whether a refresh happens, so a 401 with no such header
     * still triggers exactly one refresh here.
     */
    @Test
    fun givenA401WithNoWwwAuthenticateHeader_whenTheAuthenticatedClientCalls_thenARefreshIsStillAttempted() =
        runTest {
            var refreshCalls = 0
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val clients =
                testClients(sessionManager = sessionManager) { request ->
                    if (request.url.encodedPath == "/auth/refresh") {
                        refreshCalls++
                        respond("""{"accessToken":"a","refreshToken":"r","expiresIn":900}""", HttpStatusCode.OK, jsonHeaders())
                    } else {
                        // Deliberately no WWW-Authenticate header on this 401.
                        respond("", HttpStatusCode.Unauthorized)
                    }
                }

            clients.authenticated.get("/me")

            assertEquals(1, refreshCalls)
        }

    @Test
    fun givenEndIsCalledWhileARefreshIsInFlight_whenItCompletes_thenTheRefreshedTokensAreDiscarded() =
        runTest {
            val refreshStarted = CompletableDeferred<Unit>()
            val proceedWithRefresh = CompletableDeferred<Unit>()
            val sessionManager = testSessionManager()
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior = { _ ->
                        refreshStarted.complete(Unit)
                        proceedWithRefresh.await()
                        respond(
                            """{"accessToken":"access-1","refreshToken":"refresh-1","expiresIn":900}""",
                            HttpStatusCode.OK,
                            jsonHeaders(),
                        )
                    }
                }
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            val callJob = launch { clients.authenticated.get("/me") }
            refreshStarted.await()
            sessionManager.end()
            proceedWithRefresh.complete(Unit)
            callJob.join()
            yield()

            val status = sessionManager.status.value
            assertIs<SessionStatus.SignedOut>(status)
            assertEquals(SignOutReason.LoggedOut, status.reason)
            assertNull(sessionManager.currentTokens())
        }

    /**
     * Proves `nonCancellableRefresh = true` (ADR-0011): cancelling the caller mid-refresh must not
     * roll back a refresh that goes on to succeed, in memory or in storage, unlike the logout race
     * above where the compare-and-set itself is what discards the rotated pair.
     */
    @Test
    fun givenTheCallingJobIsCancelledDuringARefresh_whenTheRefreshCompletes_thenTheRotatedTokensAreKept() =
        runTest {
            val refreshStarted = CompletableDeferred<Unit>()
            val proceedWithRefresh = CompletableDeferred<Unit>()
            val storage = InMemorySecureStringStorage()
            val sessionManager = testSessionManager(storage)
            sessionManager.start(AuthTokens(AccessToken(STALE_ACCESS_TOKEN), RefreshToken(INITIAL_REFRESH_TOKEN)))
            val server =
                RotatingAuthServer().apply {
                    refreshBehavior = { _ ->
                        refreshStarted.complete(Unit)
                        proceedWithRefresh.await()
                        respond(
                            """{"accessToken":"access-1","refreshToken":"refresh-1","expiresIn":900}""",
                            HttpStatusCode.OK,
                            jsonHeaders(),
                        )
                    }
                }
            val clients = testClients(sessionManager = sessionManager) { request -> server.handle(this, request) }

            val callJob = launch { clients.authenticated.get("/me") }
            refreshStarted.await()
            callJob.cancel()
            proceedWithRefresh.complete(Unit)
            callJob.join()
            yield()

            val rotatedTokens = AuthTokens(AccessToken("access-1"), RefreshToken("refresh-1"))
            assertEquals(rotatedTokens, sessionManager.currentTokens())
            assertEquals(rotatedTokens, PersistentTokenStore(storage).load())
        }
}
