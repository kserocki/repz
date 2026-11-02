package com.repz.app.core.network.client

import co.touchlab.kermit.Logger
import com.repz.app.core.network.api.RefreshTokensCall
import com.repz.app.core.network.config.ApiConfig
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.TokenRefresher
import com.repz.app.core.network.session.toBearerTokens
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds

private val CONNECT_TIMEOUT = 10.seconds
private val SOCKET_TIMEOUT = 30.seconds
private val REQUEST_TIMEOUT = 30.seconds

/**
 * [public] has no [Auth] plugin, so a login's 401 (`invalid-credentials`) can never trigger a
 * refresh, and the refresh call itself (made over [public]) can't recurse into refreshing itself.
 * [authenticated] is the same client plus the bearer + refresh setup, for `/me` and the sync routes.
 * [sessionManager] travels alongside so the `/me` and sync API implementations can tell a 401 the
 * refresh flow already gave up on (session expired) apart from a plain, still-signed-in 401.
 */
class RepzHttpClients internal constructor(
    val public: HttpClient,
    val authenticated: HttpClient,
    internal val sessionManager: SessionManager,
)

fun createRepzHttpClients(
    engine: HttpClientEngine,
    config: ApiConfig,
    sessionManager: SessionManager,
    logger: Logger,
): RepzHttpClients {
    val publicClient = HttpClient(engine) { configureCommon(config, logger) }
    val tokenRefresher = TokenRefresher(sessionManager, RefreshTokensCall(publicClient))

    val authenticatedClient =
        publicClient.config {
            install(Auth) {
                bearer {
                    cacheTokens = false
                    nonCancellableRefresh = true
                    loadTokens { sessionManager.currentTokens()?.toBearerTokens() }
                    sendWithoutRequest { true }
                    refreshTokens { tokenRefresher.refresh(oldTokens) }
                }
            }
        }

    return RepzHttpClients(publicClient, authenticatedClient, sessionManager)
}

private fun HttpClientConfig<*>.configureCommon(
    config: ApiConfig,
    logger: Logger,
) {
    expectSuccess = false

    defaultRequest {
        url("${config.baseUrl}/")
    }

    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT.inWholeMilliseconds
        socketTimeoutMillis = SOCKET_TIMEOUT.inWholeMilliseconds
        requestTimeoutMillis = REQUEST_TIMEOUT.inWholeMilliseconds
    }

    install(ContentNegotiation) {
        json(NetworkJson, contentType = ContentType.Application.Json)
        json(NetworkJson, contentType = ContentType.parse("application/problem+json"))
    }

    install(Logging) {
        this.logger = kermitHttpLogger(logger)
        level = LogLevel.INFO
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}
