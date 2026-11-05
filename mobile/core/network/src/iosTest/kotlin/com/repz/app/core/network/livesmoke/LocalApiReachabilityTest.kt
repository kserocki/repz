package com.repz.app.core.network.livesmoke

import com.repz.app.core.network.api.HealthApi
import com.repz.app.core.network.api.KtorHealthApi
import com.repz.app.core.network.client.createRepzHttpClients
import com.repz.app.core.network.client.platformHttpClientEngine
import com.repz.app.core.network.config.ApiConfig
import com.repz.app.core.network.dto.health.HealthResponseDto
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.network.testing.testLogger
import com.repz.app.core.network.testing.testSessionManager
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSProcessInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val HEALTHY_STATUS = "ok"

/**
 * Opt-in only: proves the Darwin engine actually reaches a real API over cleartext local
 * networking (`NSAllowsLocalNetworking`) and the simulator's own `localhost` alias. Skips itself
 * (passes trivially) unless `REPZ_LIVE_API_URL` is set in the process environment - on the
 * simulator that means `SIMCTL_CHILD_REPZ_LIVE_API_URL` in the host shell before
 * `./gradlew :core:network:iosSimulatorArm64Test` (Spotter's manual test plan, I2), since a real
 * network call must never run as part of the normal gate.
 */
class LocalApiReachabilityTest {
    @Test
    fun givenALiveApiUrl_whenHealthIsChecked_thenTheRequestSucceeds() =
        runTest {
            val liveApiUrl = (NSProcessInfo.processInfo.environment()["REPZ_LIVE_API_URL"] as? String).orEmpty()
            if (liveApiUrl.isEmpty()) return@runTest

            val clients = createRepzHttpClients(platformHttpClientEngine(), ApiConfig(liveApiUrl), testSessionManager(), testLogger())
            val healthApi: HealthApi = KtorHealthApi(clients)

            val result = healthApi.check()

            assertIs<NetworkResult.Success<HealthResponseDto>>(result)
            assertEquals(HEALTHY_STATUS, result.value.status)
        }
}
