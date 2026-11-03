package com.repz.app.core.network.result

import com.repz.app.core.network.dto.health.HealthResponseDto
import com.repz.app.core.network.testing.testClients
import io.ktor.client.call.body
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.fail

private const val PROBLEM_JSON_CONTENT_TYPE = "application/problem+json"

private fun problemBody(
    type: String,
    status: Int,
): String = """{"type":"$type","title":"T","status":$status,"instance":"/x","requestId":"r1"}"""

class ApiCallTest {
    @Test
    fun givenA200WithAValidBody_whenApiCallRuns_thenItDecodesToSuccess() =
        runTest {
            val clients =
                testClients { respond("""{"status":"ok"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }

            val result =
                apiCall(
                    decode = { it.body<HealthResponseDto>() },
                    request = { clients.public.get("/health") },
                )

            assertIs<NetworkResult.Success<HealthResponseDto>>(result)
            assertEquals("ok", result.value.status)
        }

    @Test
    fun given204_whenApiCallRunsWithTheUnitOverload_thenItSucceedsWithUnit() =
        runTest {
            val clients = testClients { respond("", HttpStatusCode.NoContent) }

            val result = apiCall { clients.public.get("/logout") }

            assertIs<NetworkResult.Success<Unit>>(result)
        }

    @Test
    fun givenAKnownProblemType_whenApiCallRuns_thenItMapsToApiWithThatProblemType() =
        runTest {
            val clients =
                testClients {
                    respond(
                        problemBody("urn:repz:problem:invalid-credentials", 401),
                        HttpStatusCode.Unauthorized,
                        headersOf(HttpHeaders.ContentType, PROBLEM_JSON_CONTENT_TYPE),
                    )
                }

            val result = apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") })

            val error = (result as NetworkResult.Failure).error
            assertIs<NetworkError.Api>(error)
            assertEquals(401, error.status)
            assertEquals(ProblemType.InvalidCredentials, error.problem?.type)
        }

    @Test
    fun givenAnUnknownProblemUrn_whenApiCallRuns_thenItMapsToOther() =
        runTest {
            val clients =
                testClients {
                    respond(
                        problemBody("urn:repz:problem:something-new", 400),
                        HttpStatusCode.BadRequest,
                        headersOf(HttpHeaders.ContentType, PROBLEM_JSON_CONTENT_TYPE),
                    )
                }

            val result = apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") })

            val error = (result as NetworkResult.Failure).error as NetworkError.Api
            assertEquals(ProblemType.Other("urn:repz:problem:something-new"), error.problem?.type)
        }

    @Test
    fun givenAPlainAboutBlankProblem_whenApiCallRuns_thenItMapsToPlainHttp() =
        runTest {
            val clients =
                testClients {
                    respond(
                        problemBody("about:blank", 404),
                        HttpStatusCode.NotFound,
                        headersOf(HttpHeaders.ContentType, PROBLEM_JSON_CONTENT_TYPE),
                    )
                }

            val result = apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") })

            val error = (result as NetworkResult.Failure).error as NetworkError.Api
            assertEquals(ProblemType.PlainHttp, error.problem?.type)
        }

    @Test
    fun given500WithANonJsonBody_whenApiCallRuns_thenProblemIsNull() =
        runTest {
            val clients = testClients { respondError(HttpStatusCode.InternalServerError, content = "oops") }

            val result = apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") })

            val error = (result as NetworkResult.Failure).error as NetworkError.Api
            assertEquals(500, error.status)
            assertNull(error.problem)
        }

    @Test
    fun givenA2xxBodyThatDoesNotMatchTheExpectedShape_whenApiCallRuns_thenItIsMalformedResponse() =
        runTest {
            val clients =
                testClients {
                    respond("""{"unexpected":true}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }

            val result =
                apiCall(
                    decode = { it.body<HealthResponseDto>() },
                    request = { clients.public.get("/health") },
                )

            assertIs<NetworkError.MalformedResponse>((result as NetworkResult.Failure).error)
        }

    @Test
    fun givenARequestTimeout_whenApiCallRuns_thenItMapsToTimeout() =
        runTest {
            val clients = testClients { throw HttpRequestTimeoutException(url = it.url.toString(), timeoutMillis = 1000) }

            val result = apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") })

            assertEquals(NetworkError.Timeout, (result as NetworkResult.Failure).error)
        }

    /**
     * `kotlinx.io.IOException` is the common-code "no route to the server" transport failure: a
     * `typealias` for `java.io.IOException` on the JVM, and matched directly by the Darwin `actual`
     * (`TransportFailure.ios.kt`), so this one test exercises both platforms' mappings.
     */
    @Test
    fun givenATransportIoException_whenApiCallRuns_thenItMapsToOffline() =
        runTest {
            val clients = testClients { throw IOException("no route to the server") }

            val result = apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") })

            assertEquals(NetworkError.Offline, (result as NetworkResult.Failure).error)
        }

    @Test
    fun givenTheCallingJobIsCancelled_whenApiCallRuns_thenCancellationPropagatesInsteadOfAFailure() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val clients =
                testClients {
                    started.complete(Unit)
                    awaitCancellation()
                }

            val deferred = async { apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") }) }
            started.await()
            deferred.cancel()

            assertFailsWith<CancellationException> { deferred.await() }
        }

    @Test
    fun givenAnUnknownExceptionFromTheEngine_whenApiCallRuns_thenItPropagatesInsteadOfBecomingAFailure() =
        runTest {
            val clients = testClients { throw IllegalStateException("boom") }

            try {
                apiCall(decode = { it.body<String>() }, request = { clients.public.get("/x") })
                fail("expected IllegalStateException to propagate")
            } catch (e: IllegalStateException) {
                assertEquals("boom", e.message)
            }
        }
}
