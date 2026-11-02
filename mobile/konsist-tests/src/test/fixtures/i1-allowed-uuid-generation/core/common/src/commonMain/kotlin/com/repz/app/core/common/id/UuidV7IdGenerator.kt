package com.repz.app.core.common.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UuidV7IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    fun newId(): Uuid = Uuid.generateV7()
}
