package com.repz.app.koinprobe

import com.repz.app.core.testing.koin.KoinProbeDependency
import com.repz.app.core.testing.koin.KoinProbeProviderModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

/**
 * Consumer side of the cross-module Koin regression probe. Deliberately outside
 * `com.repz.app.shared` so [AppModule][com.repz.app.shared.di.AppModule]'s `@ComponentScan` can
 * never pick these test classes up when `RepzApp` is assembled.
 */
@Module
@ComponentScan("com.repz.app.koinprobe")
class KoinProbeConsumerModule

@Factory
class KoinProbeConsumer(
    val dependency: KoinProbeDependency,
)

@org.koin.core.annotation.KoinApplication(modules = [KoinProbeProviderModule::class, KoinProbeConsumerModule::class])
object KoinProbeApp
