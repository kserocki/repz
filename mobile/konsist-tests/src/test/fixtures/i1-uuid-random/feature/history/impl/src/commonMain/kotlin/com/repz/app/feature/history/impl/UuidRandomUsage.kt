package com.repz.app.feature.history.impl

import kotlin.uuid.Uuid

class UuidRandomUsage {
    fun newId(): Uuid = Uuid.random()
}
