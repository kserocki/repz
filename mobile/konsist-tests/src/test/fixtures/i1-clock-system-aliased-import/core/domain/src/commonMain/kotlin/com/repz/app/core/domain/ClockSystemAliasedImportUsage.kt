package com.repz.app.core.domain

import kotlin.time.Clock.System as TheSystemClock

class ClockSystemAliasedImportUsage {
    fun now() = TheSystemClock.now()
}
