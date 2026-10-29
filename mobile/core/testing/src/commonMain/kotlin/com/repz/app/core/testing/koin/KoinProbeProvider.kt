package com.repz.app.core.testing.koin

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Provider side of the permanent cross-module Koin regression probe (see ADR-0008 for why this
 * is an explicit provider function, not `@ComponentScan`). A consumer in `app:shared`'s
 * `commonTest` depends on [KoinProbeDependency], declared in this separate Gradle module.
 */
@Module
class KoinProbeProviderModule {
    @Single
    fun koinProbeDependency(): KoinProbeDependency = KoinProbeDependency()
}

class KoinProbeDependency
