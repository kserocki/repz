package com.repz.app.core.domain

import kotlin.time.Clock

class ClockSystemUsage {
    fun now() = Clock.System.now()
}
