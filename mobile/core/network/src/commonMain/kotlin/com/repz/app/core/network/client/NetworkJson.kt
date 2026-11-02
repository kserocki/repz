package com.repz.app.core.network.client

import kotlinx.serialization.json.Json

/**
 * Production JSON config. `encodeDefaults`/`explicitNulls` so a required-but-nullable field (e.g.
 * `templateId`, `rpe`) or a field with a default (e.g. `notes`) is still sent on the wire, matching
 * the OpenAPI contract exactly. `ignoreUnknownKeys` tolerates an additive server field reaching an
 * older installed build; unknown **enum values** still fail to decode (Q2: strict), because a vocabulary
 * serializer has no "unknown" fallback value to coerce into.
 */
internal val NetworkJson =
    Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
        isLenient = false
        coerceInputValues = false
    }

/** Same wire shapes, but rejects an unknown key too, so a contract fixture drifting from the DTOs fails loudly. */
internal val StrictContractJson = Json(from = NetworkJson) { ignoreUnknownKeys = false }
