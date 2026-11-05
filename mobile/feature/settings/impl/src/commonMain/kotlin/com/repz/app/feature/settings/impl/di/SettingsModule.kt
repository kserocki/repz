package com.repz.app.feature.settings.impl.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** Every definition is class-level (ADR-0008): `@Factory` use cases, `@KoinViewModel` ViewModels. */
@Module
@ComponentScan("com.repz.app.feature.settings.impl")
class SettingsModule
