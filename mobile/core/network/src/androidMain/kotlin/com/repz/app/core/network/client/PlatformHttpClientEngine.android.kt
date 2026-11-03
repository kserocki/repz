package com.repz.app.core.network.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * OkHttp never installs an HTTP cache unless a [okhttp3.Cache] is explicitly configured, but
 * `cache(null)` is set anyway so this stays true even if a later OkHttp default changes (ADR-0011:
 * the server already sends `Cache-Control: no-store` on every endpoint this client calls).
 */
internal actual fun platformHttpClientEngine(): HttpClientEngine =
    OkHttp.create {
        config {
            cache(null)
        }
    }
