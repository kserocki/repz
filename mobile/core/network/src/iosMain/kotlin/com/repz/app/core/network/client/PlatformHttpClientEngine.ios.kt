package com.repz.app.core.network.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData

/**
 * Darwin (`NSURLSession`) with caching disabled at the session level: the server already sends
 * `Cache-Control: no-store` on every auth and sync route, this just makes the client independent of
 * `NSURLSession`'s own shared cache (§3.2).
 */
internal actual fun platformHttpClientEngine(): HttpClientEngine =
    Darwin.create {
        configureSession {
            URLCache = null
            requestCachePolicy = NSURLRequestReloadIgnoringLocalCacheData
        }
    }
