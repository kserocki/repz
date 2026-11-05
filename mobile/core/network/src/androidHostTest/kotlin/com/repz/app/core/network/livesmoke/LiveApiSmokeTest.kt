package com.repz.app.core.network.livesmoke

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.repz.app.core.common.id.UuidV7IdGenerator
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
import com.repz.app.core.network.result.NetworkError
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
private const val HEX_RADIX = 16

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

private fun randomSmokeEmail(): String = "smoke-${Random.nextLong().toString(HEX_RADIX).removePrefix("-")}@example.com"

private val smokeIdGenerator = UuidV7IdGenerator()

private fun randomSmokeUuid(): kotlin.uuid.Uuid = smokeIdGenerator.newId()

/** One throwaway session against the real API: its own [SessionManager], real engine, real clients. */
private class LiveApiSession(
    baseUrl: String,
    logWriter: LogWriter,
) {
    private val logger = Logger(StaticConfig(logWriterList = listOf(logWriter)))
    val sessionManager = SessionManager(PersistentTokenStore(InMemorySecureStringStorage()), logger)
    private val clients = createRepzHttpClients(platformHttpClientEngine(), ApiConfig(baseUrl), sessionManager, logger)
    val authApi: AuthApi = KtorAuthApi(clients)
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
        runBlocking<Unit> {
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
        runBlocking<Unit> {
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
        runBlocking<Unit> {
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

    // L4
    @Test
    fun givenARefreshToken_whenItIsUsedTwiceWithinTheGraceWindow_thenBothCallsSucceed() =
        runBlocking<Unit> {
            requireLiveApi()
            val session = LiveApiSession(liveApiUrl, logWriter)
            val email = randomSmokeEmail()
            val authSession = (session.authApi.register(RegisterRequestDto(email, SMOKE_PASSWORD, "Smoke")) as NetworkResult.Success).value
            session.sessionManager.start(authSession.toAuthTokens())
            val refreshToken = session.sessionManager.currentRefreshToken()!!

            // The second call replays the same (already rotated) token well inside the 30 s grace
            // window (ADR-0011): the server issues a new pair instead of revoking the family.
            val firstRefresh = session.refreshTokensCall(refreshToken)
            val secondRefresh = session.refreshTokensCall(refreshToken)

            assertTrue(firstRefresh is NetworkResult.Success, "first refresh failed: $firstRefresh")
            assertTrue(secondRefresh is NetworkResult.Success, "second refresh failed: $secondRefresh")
        }

    // L5
    @Test
    fun givenASessionRevokedServerSide_whenGetMeIsCalledAfterTheAccessTokenExpires_thenTheResultIsSessionExpired() =
        runBlocking<Unit> {
            requireLiveApi()
            val session = LiveApiSession(liveApiUrl, logWriter)
            val email = randomSmokeEmail()
            val authSession = (session.authApi.register(RegisterRequestDto(email, SMOKE_PASSWORD, "Smoke")) as NetworkResult.Success).value
            session.sessionManager.start(authSession.toAuthTokens())
            val refreshToken = session.sessionManager.currentRefreshToken()!!

            // Revokes the refresh token server-side only (no local end()): the client still believes
            // it's signed in, the same as another device logging this session out remotely.
            session.authApi.logout(refreshToken)
            delay((accessTtlSeconds + 1).seconds)

            val result = session.userApi.getMe()

            val failure = assertIs<NetworkResult.Failure>(result)
            assertEquals(NetworkError.SessionExpired, failure.error)
            val status = session.sessionManager.status.value
            assertIs<SessionStatus.SignedOut>(status)
            assertEquals(SignOutReason.Expired, status.reason)
            assertNull(session.sessionManager.currentTokens())
        }

    // L6 - opt-in like the rest of the class, but meant to be run on its own
    // (`--tests '*givenStoppedApi*'`) after Spotter has stopped the live API: every other test in
    // this class needs the API up, so a full-class run would otherwise fail this one too.
    @Test
    fun givenStoppedApi_whenACallIsMade_thenTheResultIsOffline() =
        runBlocking<Unit> {
            requireLiveApi()
            val session = LiveApiSession(liveApiUrl, logWriter)

            val result = session.authApi.register(RegisterRequestDto(randomSmokeEmail(), SMOKE_PASSWORD, "Smoke"))

            val failure = assertIs<NetworkResult.Failure>(result)
            assertEquals(NetworkError.Offline, failure.error)
        }

    private fun assertNoSecretsLogged(vararg secrets: String) {
        val logged = logWriter.lines.joinToString("\n")
        secrets.forEach { secret -> assertTrue(secret !in logged, "A secret leaked into the log: $secret") }
    }

    private companion object {
        const val DEFAULT_ACCESS_TTL_SECONDS = 900L
    }
}
