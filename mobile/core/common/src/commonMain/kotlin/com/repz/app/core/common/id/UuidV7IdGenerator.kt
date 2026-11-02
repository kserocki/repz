package com.repz.app.core.common.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The real [IdGenerator], producing time-ordered UUIDv7 ids (ADR-0022). Time ordering helps
 * B-tree locality in both Room and Postgres, which already mints its own rows with `uuidv7()`.
 * `generateV7` is still `@ExperimentalUuidApi` on Kotlin 2.4.21, hence the scoped opt-in.
 */
class UuidV7IdGenerator : IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun newId(): Uuid = Uuid.generateV7()
}
