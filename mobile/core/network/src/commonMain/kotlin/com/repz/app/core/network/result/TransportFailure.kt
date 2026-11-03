package com.repz.app.core.network.result

/**
 * Maps a transport-level exception (no HTTP response at all) to [NetworkError.Offline] or
 * [NetworkError.Timeout]; `null` when [this] isn't a transport failure this platform recognizes,
 * so the caller rethrows it (a programming error must never be hidden as `Offline`).
 */
internal expect fun Throwable.toTransportFailure(): NetworkError?
