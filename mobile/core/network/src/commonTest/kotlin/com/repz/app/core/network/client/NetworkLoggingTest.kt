package com.repz.app.core.network.client

import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.auth.LoginRequestDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.PushRequestDto
import com.repz.app.core.network.session.AccessToken
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.RefreshToken
import com.repz.app.core.network.testing.RecordingLogWriter
import com.repz.app.core.network.testing.testClients
import com.repz.app.core.network.testing.testLogger
import com.repz.app.core.network.testing.testSessionManager
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private const val PASSWORD = "correct horse battery staple"
private const val ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.secret-access-token-body"
private const val REFRESH_TOKEN = "some-secret-refresh-token"

private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

/** A fake server with just enough behaviour to run a login, a 401-then-refresh-then-retry push, and a plain 404 for anything else. */
private class LoggingTestServer {
    private var authenticatedCallCount = 0

    suspend fun handle(
        scope: MockRequestHandleScope,
        request: HttpRequestData,
    ): HttpResponseData =
        when (request.url.encodedPath) {
            "/auth/login" -> scope.respondSession()
            "/auth/refresh" -> scope.respondRefreshedTokens()
            "/sync/push" -> scope.respondToPush()
            else -> scope.respond("not found", HttpStatusCode.NotFound)
        }

    private fun MockRequestHandleScope.respondSession(): HttpResponseData =
        respond(
            """{"user":{"id":"b6e6b6a0-0000-4a22-8a33-000000000001","email":"a@b.com",""" +
                """"displayName":"A","unitSystem":"kg"},""" +
                """"accessToken":"$ACCESS_TOKEN","refreshToken":"$REFRESH_TOKEN","expiresIn":900}""",
            HttpStatusCode.OK,
            jsonHeaders(),
        )

    private fun MockRequestHandleScope.respondRefreshedTokens(): HttpResponseData =
        respond(
            """{"accessToken":"$ACCESS_TOKEN-2","refreshToken":"$REFRESH_TOKEN-2","expiresIn":900}""",
            HttpStatusCode.OK,
            jsonHeaders(),
        )

    private fun MockRequestHandleScope.respondToPush(): HttpResponseData {
        authenticatedCallCount++
        return if (authenticatedCallCount == 1) {
            respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
        } else {
            respond("""{"accepted":[],"rejected":[]}""", HttpStatusCode.OK, jsonHeaders())
        }
    }
}

private fun samplePushRequest(): PushRequestDto =
    PushRequestDto(listOf(PushChangeDto.Delete(SyncTypeDto.WORKOUT, Uuid.parse("b6e6b6a0-0000-4a22-8a33-000000000099"))))

class NetworkLoggingTest {
    @Test
    fun givenALoginAThenASync401RefreshAndRetryThenAPush_whenLoggingAtInfo_thenNoSecretEverAppears() =
        runTest {
            val writer = RecordingLogWriter()
            val sessionManager = testSessionManager()
            val server = LoggingTestServer()

            val clients =
                testClients(sessionManager = sessionManager, logger = testLogger(writer)) { request -> server.handle(this, request) }

            sessionManager.start(AuthTokens(AccessToken("stale"), RefreshToken("stale-refresh")))
            clients.public.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto("a@b.com", PASSWORD))
            }
            clients.authenticated.post("/sync/push") {
                contentType(ContentType.Application.Json)
                setBody(samplePushRequest())
            }
            clients.public.get("/health")

            val logged = writer.messages.joinToString("\n")
            assertTrue(logged.contains("/auth/login"))
            assertTrue(logged.contains("/sync/push"))
            assertFalse(logged.contains(PASSWORD))
            assertFalse(logged.contains(ACCESS_TOKEN))
            assertFalse(logged.contains(REFRESH_TOKEN))
            assertFalse(logged.contains(HttpHeaders.Authorization))
        }
}
