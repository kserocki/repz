package com.repz.app.core.network.livesmoke

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import co.touchlab.kermit.Logger
import com.repz.app.core.network.api.HealthApi
import com.repz.app.core.network.api.KtorHealthApi
import com.repz.app.core.network.client.NETWORK_LOG_TAG
import com.repz.app.core.network.client.SESSION_LOG_TAG
import com.repz.app.core.network.client.createRepzHttpClients
import com.repz.app.core.network.client.platformHttpClientEngine
import com.repz.app.core.network.config.ApiConfig
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.session.PersistentTokenStore
import com.repz.app.core.network.session.SecureStringStorage
import com.repz.app.core.network.session.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val LOCAL_API_BASE_URL = "http://10.0.2.2:3000"
private const val LIVE_API_INSTRUMENTATION_ARGUMENT = "repzLiveApi"

/** No persistence needed: this test never signs in, only reads `/health` on the public client. */
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

/**
 * Opt-in only: run with `-Pandroid.testInstrumentationRunnerArguments.repzLiveApi=true` against a
 * locally running API (`pnpm dev` from `api/`, port 3000). Proves the emulator can actually reach
 * it through the real `INTERNET` permission, the `networkSecurityConfig` cleartext allowance for
 * `10.0.2.2`, and the real OkHttp engine — none of which a host-side test can observe. Passes
 * trivially otherwise, so it never needs an API (and never runs in CI: no emulator there).
 */
@RunWith(AndroidJUnit4::class)
class LocalApiReachabilityTest {
    @Test
    fun givenTheLiveApiArgumentIsSet_whenHealthIsChecked_thenTheApiRespondsSuccessfully() =
        runBlocking {
            val liveApiRequested =
                InstrumentationRegistry.getArguments().getString(LIVE_API_INSTRUMENTATION_ARGUMENT).toBoolean()
            if (!liveApiRequested) return@runBlocking

            val sessionManager = SessionManager(PersistentTokenStore(InMemorySecureStringStorage()), Logger.withTag(SESSION_LOG_TAG))
            val clients =
                createRepzHttpClients(
                    engine = platformHttpClientEngine(),
                    config = ApiConfig(LOCAL_API_BASE_URL),
                    sessionManager = sessionManager,
                    logger = Logger.withTag(NETWORK_LOG_TAG),
                )
            val healthApi: HealthApi = KtorHealthApi(clients.public)

            val result = healthApi.check()

            assertTrue("Expected a successful /health response, got $result", result is NetworkResult.Success<*>)
        }
}
