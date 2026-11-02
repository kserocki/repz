package com.repz.app.core.network.config

/**
 * The default base URL for this platform's emulator/simulator (a physical-device override is a
 * follow-up issue): Android's `10.0.2.2` loopback alias to the host, or iOS simulator's `localhost`.
 */
internal expect fun platformDefaultApiBaseUrl(): String
