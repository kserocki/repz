package com.repz.app.core.network.livesmoke

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.repz.app.core.network.api.AuthApi
import com.repz.app.core.network.api.KtorAuthApi
import com.repz.app.core.network.api.KtorSyncApi
import com.repz.app.core.network.api.KtorUserApi
import com.repz.app.core.network.api.RefreshTokensCall
import com.repz.app.core.network.api.SyncApi
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.client.createRepzHttpClients
import com.repz.app.core.network.client.platformHttpClientEngine
import com.repz.app.core.network.config.ApiConfig
import com.repz.app.core.network.dto.SetTypeDto
import com.repz.app.core.network.dto.auth.RegisterRequestDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.PushRequestDto
import com.repz.app.core.network.dto.sync.WorkoutExercisePayloadDto
import com.repz.app.core.network.dto.sync.WorkoutPayloadDto
import com.repz.app.core.network.dto.sync.WorkoutSetPayloadDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.session.AuthTokens
import com.repz.app.core.network.session.PersistentTokenStore
import com.repz.app.core.network.session.SecureStringStorage
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.SessionStatus
import com.repz.app.core.network.session.SignOutReason
import com.repz.app.core.network.session.toAuthTokens
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val SMOKE_PASSWORD = "correct horse battery staple smoke test"

private class InMemorySecureStringStorage : SecureStringStorage {
    private var stored: String? = null

    override suspend fun read(): String? = stored

    override suspend fun write(value: String): Boolean {
        stored = value
        return true
    }

    override suspend fun delete() {
        stored = null
    }
}

private class CapturingLogWriter : LogWriter() {
    val lines = mutableListOf<String>()

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        lines += message
    }
}

private fun randomSmokeEmail(): String = "smoke-${Random.nextLong().toString(16).removePrefix("-")}@example.com"

/**
 * A random-looking id without calling the id generator's own `Uuid.generateV7()`/`Uuid.random()`
 * (Konsist I1 restricts those to `UuidV7IdGenerator`, tests included): bytes come from
 * [kotlin.random.Random], then are only reformatted into the standard UUID string shape.
 */
private fun randomSmokeUuid(): kotlin.uuid.Uuid {
    val hex = Random.nextBytes(16).joinToString("") { byte -> ((byte.toInt() and 0xFF) or 0x100).toString(16).substring(1) }
    val formatted =
        "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
    return kotlin.uuid.Uuid.parse(formatted)
}

/** One throwaway session against the real API: its own [SessionManager], real engine, real clients. */
private class LiveApiSession(
    baseUrl: String,
    logWriter: LogWriter,
) {
    private val logger = Logger(StaticConfig(logWriterList = listOf(logWriter)))
    val sessionManager = SessionManager(PersistentTokenStore(InMemorySecureStringStorage()), logger)
    private val clients = createRepzHttpClients(platformHttpClientEngine(), ApiConfig(baseUrl), sessionManager, logger)
    val authApi: AuthApi = KtorAuthApi(clients.public)
    val userApi: UserApi = KtorUserApi(clients)
    val syncApi: SyncApi = KtorSyncApi(clients)
    val refreshTokensCall = RefreshTokensCall(clients.public)
}

/**
 * Opt-in only (`-Prepz.liveApiUrl=<url>`, excluded otherwise - see the build file): exercises a real
 * running API over the real platform engine. Spotter's manual test plan (L1-L6) runs this with a
 * short access-token TTL (`ACCESS_TOKEN_TTL_SECONDS=3`) and greps the API's own log for the
 * exactly-one-refresh and grace-window assertions this test can't see from the client side.
 */
class LiveApiSmokeTest {
    private val liveApiUrl: String = System.getProperty("repz.liveApiUrl").orEmpty()
    private val accessTtlSeconds: Long = System.getProperty("repz.liveApiAccessTtlSeconds")?.toLongOrNull() ?: DEFAULT_ACCESS_TTL_SECONDS
    private val logWriter = CapturingLogWriter()

    private fun requireLiveApi() {
        check(liveApiUrl.isNotEmpty()) { "LiveApiSmokeTest needs -Prepz.liveApiUrl=<url>" }
    }

    // L1
    @Test
    fun givenANewlyRegisteredUser_whenGetMeIsCalled_thenTheEmailMatches() =
        runBlocking {
            requireLiveApi()
            val session = LiveApiSession(liveApiUrl, logWriter)
            val email = randomSmokeEmail()

            val registered = session.authApi.register(RegisterRequestDto(email, SMOKE_PASSWORD, "Smoke"))
            val authSession = (registered as NetworkResult.Success).value
            session.sessionManager.start(authSession.toAuthTokens())

            val me = (session.userApi.getMe() as NetworkResult.Success).value
            assertEquals(email, me.email)
            assertNoSecretsLogged(SMOKE_PASSWORD, authSession.accessToken, authSession.refreshToken)
        }

    // L2
    @Test
    fun givenAnExpiredAccessToken_whenFiveConcurrentGetMeCallsRun_thenAllFiveSucceed() =
        runBlocking {
            requireLiveApi()
            val session = LiveApiSession(liveApiUrl, logWriter)
            val email = randomSmokeEmail()
            val authSession = (session.authApi.register(RegisterRequestDto(email, SMOKE_PASSWORD, "Smoke")) as NetworkResult.Success).value
            session.sessionManager.start(authSession.toAuthTokens())

            delay((accessTtlSeconds + 1).seconds)

            val results = (1..5).map { async { session.userApi.getMe() } }.awaitAll()

            assertTrue(results.all { it is NetworkResult.Success })
        }

    // L3
    @Test
    fun givenAPushedWorkout_whenPulledBackFromCursorZero_thenTheSamePayloadComesBack() =
        runBlocking {
            requireLiveApi()
            val session = LiveApiSession(liveApiUrl, logWriter)
            val email = randomSmokeEmail()
            val authSession = (session.authApi.register(RegisterRequestDto(email, SMOKE_PASSWORD, "Smoke")) as NetworkResult.Success).value
            session.sessionManager.start(authSession.toAuthTokens())

            val workoutId = randomSmokeUuid()
            val payload =
                WorkoutPayloadDto(
                    templateId = null,
                    name = "Smoke workout",
                    startedAt = Instant.parse("2026-01-01T10:00:00.000Z"),
                    finishedAt = Instant.parse("2026-01-01T10:30:00.000Z"),
                    exercises =
                        listOf(
                            WorkoutExercisePayloadDto(
                                id = randomSmokeUuid(),
                                exerciseId = randomSmokeUuid(),
                                sets =
                                    listOf(
                                        WorkoutSetPayloadDto(
                                            id = randomSmokeUuid(),
                                            type = SetTypeDto.NORMAL,
                                            weightGrams = 60_000,
                                            reps = 8,
                                            rpe = 8.0,
                                            completedAt = Instant.parse("2026-01-01T10:05:00.000Z"),
                                        ),
                                    ),
                            ),
                        ),
                )

            val pushResult =
                session.syncApi.push(PushRequestDto(listOf(PushChangeDto.UpsertWorkout(workoutId, payload))))
            val accepted = (pushResult as NetworkResult.Success).value.accepted.single()
            assertEquals(workoutId, accepted.id)
            assertTrue(accepted.version != null)

            val pullResult = session.syncApi.pull(cursor = 0)
            val pulledWorkout =
                (pullResult as NetworkResult.Success)
                    .value.changes
                    .filterIsInstance<com.repz.app.core.network.dto.sync.SyncChangeDto.Workout>()
                    .single { it.id == workoutId }

            assertEquals(payload, pulledWorkout.payload)
        }

    // L5
    @Test
    fun givenALoggedOutSession_whenACallIsMadeAfterTheTokenExpires_thenItIsSessionExpired() =
        runBlocking {
            requireLiveApi()
            val session = LiveApiSession(liveApiUrl, logWriter)
            val email = randomSmokeEmail()
            val authSession = (session.authApi.register(RegisterRequestDto(email, SMOKE_PASSWORD, "Smoke")) as NetworkResult.Success).value
            session.sessionManager.start(authSession.toAuthTokens())
            val refreshToken = session.sessionManager.currentRefreshToken()!!

            session.authApi.logout(refreshToken)
            session.sessionManager.end()
            delay((accessTtlSeconds + 1).seconds)
            // A logged-out refresh token is rejected the same way an expired one is (REP-25 only
            // distinguishes "signed out" from "expired" client-side; the server returns the same 401).
            val refreshed = session.refreshTokensCall(refreshToken)

            assertIs<NetworkResult.Failure>(refreshed)
        }

    private fun assertNoSecretsLogged(vararg secrets: String) {
        val logged = logWriter.lines.joinToString("\n")
        secrets.forEach { secret -> assertTrue(secret !in logged, "A secret leaked into the log: $secret") }
    }

    private companion object {
        const val DEFAULT_ACCESS_TTL_SECONDS = 900L
    }
}

