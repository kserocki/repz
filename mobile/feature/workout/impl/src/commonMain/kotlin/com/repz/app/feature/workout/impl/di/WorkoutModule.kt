package com.repz.app.feature.workout.impl.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** Every definition is class-level (ADR-0008): `@Single` the write lock, `@Factory` use cases, `@KoinViewModel` ViewModels. */
@Module
@ComponentScan("com.repz.app.feature.workout.impl")
class WorkoutModule
