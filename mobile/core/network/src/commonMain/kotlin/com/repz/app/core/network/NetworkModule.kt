package com.repz.app.core.network

import co.touchlab.kermit.Logger
import com.repz.app.core.network.client.NETWORK_LOG_TAG
import com.repz.app.core.network.client.RepzHttpClients
import com.repz.app.core.network.client.createRepzHttpClients
import com.repz.app.core.network.client.platformHttpClientEngine
import com.repz.app.core.network.config.ApiConfig
import com.repz.app.core.network.config.platformDefaultApiBaseUrl
import com.repz.app.core.network.session.SessionManager
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * `SecureStringStorage` is the one binding this module doesn't provide: it needs an Android
 * `Context` (S1, ADR-0008) and is wired in `app:shared`'s `PlatformModule` instead; Koin resolves
 * it across the two modules the same way the existing cross-module probe does.
 *
 * Every other binding is a class-level `@Single` picked up by the scan (`TokenStore`,
 * `SessionManager`, `AuthApi`, `UserApi`, `SyncApi`, `HealthApi`, REP-58). What's left here can't be
 * annotated: [ApiConfig] is a plain value built from a platform function, and [RepzHttpClients] has
 * an `internal constructor` and is built by [createRepzHttpClients] (ADR-0008 reason 1).
 */
@Module
@ComponentScan("com.repz.app.core.network")
class NetworkModule {
    @Single
    fun apiConfig(): ApiConfig = ApiConfig(baseUrl = platformDefaultApiBaseUrl())

    @Single
    fun repzHttpClients(
        config: ApiConfig,
        sessionManager: SessionManager,
    ): RepzHttpClients = createRepzHttpClients(platformHttpClientEngine(), config, sessionManager, Logger.withTag(NETWORK_LOG_TAG))
}
