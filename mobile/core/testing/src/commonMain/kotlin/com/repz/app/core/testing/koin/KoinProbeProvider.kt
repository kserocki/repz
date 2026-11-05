package com.repz.app.core.testing.koin

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Provider side of the permanent cross-module Koin regression probe (ADR-0008). A consumer in
 * `app:shared`'s `commonTest` resolves [KoinProbeDependency] (scanned, auto-bound to
 * [KoinProbeContract]) and [KoinProbeProvidedDependency] (an explicit provider), both declared in
 * this separate Gradle module, proving both definition shapes cross a module boundary on every
 * target, including Kotlin/Native. [koinProbeProvidedDependency]'s literal argument keeps this a
 * real provider (an `ApiConfig`/`RepzHttpClients`-style value, not a bare pass-through Konsist D6
 * would itself flag).
 */
@Module
@ComponentScan("com.repz.app.core.testing")
class KoinProbeProviderModule {
    @Single
    fun koinProbeProvidedDependency(): KoinProbeProvidedDependency = KoinProbeProvidedDependency(label = "koin-probe-provided")
}

interface KoinProbeContract

@Single
class KoinProbeDependency : KoinProbeContract

class KoinProbeProvidedDependency(
    val label: String,
)
