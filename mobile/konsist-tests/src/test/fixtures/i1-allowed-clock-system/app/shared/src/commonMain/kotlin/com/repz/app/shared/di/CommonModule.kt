package com.repz.app.shared.di

import kotlin.time.Clock

class CommonModule {
    fun clock(): Clock = Clock.System
}
