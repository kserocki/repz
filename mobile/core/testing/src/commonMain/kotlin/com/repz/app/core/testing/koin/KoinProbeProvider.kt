package com.repz.app.core.testing.koin

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Provider side of the permanent cross-module Koin regression probe (REP-7). A consumer in
 * `app:shared`'s `commonTest` depends on [KoinProbeDependency], proving the compiler plugin reads
 * this module's explicit provider function from another Gradle module's compiled klib. No
 * `@ComponentScan` here: cross-module component scanning drops its hints on Kotlin/Native
 * (koin-compiler-plugin#106), so every module exposes its definitions as explicit provider
 * functions instead (ADR-0008, option A).
 */
@Module
class KoinProbeProviderModule {
    @Single
    fun koinProbeDependency(): KoinProbeDependency = KoinProbeDependency()
}

class KoinProbeDependency
