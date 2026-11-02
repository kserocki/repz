package com.repz.app.core.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UuidGenerateV4Usage {
    @OptIn(ExperimentalUuidApi::class)
    fun newId(): Uuid = Uuid.generateV4()
}
