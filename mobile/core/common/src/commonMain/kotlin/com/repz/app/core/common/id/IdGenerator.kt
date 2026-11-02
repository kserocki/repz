package com.repz.app.core.common.id

import kotlin.uuid.Uuid

/**
 * Mints a new [Uuid] for a freshly created entity. Logic never calls `Uuid` generation directly
 * (konsist I1); it receives an [IdGenerator] instead, so tests can inject a deterministic one.
 */
fun interface IdGenerator {
    fun newId(): Uuid
}
