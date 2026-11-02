package com.repz.app.feature.history.impl.di

import kotlin.time.Clock

class CommonModule {
    fun clock(): Clock = Clock.System
}
