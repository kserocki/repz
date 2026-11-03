package com.repz.app.core.network.config

/** The iOS simulator reaches the host machine through its own loopback address. */
internal actual fun platformDefaultApiBaseUrl(): String = "http://localhost:3000"
