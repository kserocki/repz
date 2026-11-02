package com.repz.app.core.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UuidGenerateV7Usage {
    @OptIn(ExperimentalUuidApi::class)
    fun newId(): Uuid = Uuid.generateV7()
}
