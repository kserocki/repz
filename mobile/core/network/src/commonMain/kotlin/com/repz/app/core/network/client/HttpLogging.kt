package com.repz.app.core.network.client

import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.plugins.logging.Logger as KtorLogger

internal const val NETWORK_LOG_TAG = "Network"
internal const val SESSION_LOG_TAG = "Session"

/** Adapts a Kermit logger to Ktor's own logging interface, at `info` level. */
internal fun kermitHttpLogger(logger: KermitLogger): KtorLogger =
    object : KtorLogger {
        override fun log(message: String) {
            logger.i { message }
        }
    }
