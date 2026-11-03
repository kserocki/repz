package com.repz.app.core.network.client

import io.ktor.client.engine.HttpClientEngine

/** OkHttp on Android, Darwin on iOS: the two first-class engines (ADR-0011), never CIO. */
internal expect fun platformHttpClientEngine(): HttpClientEngine
