package com.repz.app.core.network

import co.touchlab.kermit.Logger
import com.repz.app.core.network.api.AuthApi
import com.repz.app.core.network.api.HealthApi
import com.repz.app.core.network.api.KtorAuthApi
import com.repz.app.core.network.api.KtorHealthApi
import com.repz.app.core.network.api.KtorSyncApi
import com.repz.app.core.network.api.KtorUserApi
import com.repz.app.core.network.api.SyncApi
import com.repz.app.core.network.api.UserApi
import com.repz.app.core.network.client.NETWORK_LOG_TAG
import com.repz.app.core.network.client.RepzHttpClients
import com.repz.app.core.network.client.SESSION_LOG_TAG
import com.repz.app.core.network.client.createRepzHttpClients
import com.repz.app.core.network.client.platformHttpClientEngine
import com.repz.app.core.network.config.ApiConfig
import com.repz.app.core.network.config.platformDefaultApiBaseUrl
import com.repz.app.core.network.session.PersistentTokenStore
import com.repz.app.core.network.session.SecureStringStorage
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.TokenStore
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * `SecureStringStorage` is the one binding this module doesn't provide: it needs an Android
 * `Context` (S1, ADR-0008) and is wired in `app:shared`'s `PlatformModule` instead; Koin resolves
 * it across the two modules the same way the existing cross-module probe does.
 */
@Module
class NetworkModule {
    @Single
    fun apiConfig(): ApiConfig = ApiConfig(baseUrl = platformDefaultApiBaseUrl())

    @Single
    fun tokenStore(storage: SecureStringStorage): TokenStore = PersistentTokenStore(storage)

    @Single
    fun sessionManager(tokenStore: TokenStore): SessionManager = SessionManager(tokenStore, Logger.withTag(SESSION_LOG_TAG))

    @Single
    fun repzHttpClients(
        config: ApiConfig,
        sessionManager: SessionManager,
    ): RepzHttpClients = createRepzHttpClients(platformHttpClientEngine(), config, sessionManager, Logger.withTag(NETWORK_LOG_TAG))

    @Single
    fun authApi(clients: RepzHttpClients): AuthApi = KtorAuthApi(clients.public)

    @Single
    fun userApi(clients: RepzHttpClients): UserApi = KtorUserApi(clients)

    @Single
    fun syncApi(clients: RepzHttpClients): SyncApi = KtorSyncApi(clients)

    @Single
    fun healthApi(clients: RepzHttpClients): HealthApi = KtorHealthApi(clients.public)
}
