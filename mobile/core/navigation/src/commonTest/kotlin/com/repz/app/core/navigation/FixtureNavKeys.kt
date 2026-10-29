package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.plus

/**
 * Test-only [NavKey]s standing in for two separately declared `feature:x:api` modules, proving
 * that [navKeySerializers] from different modules combine with `+` on both the JVM/Android host
 * and Kotlin/Native. [fixtureFirstModuleNavKeySerializers] and [fixtureSecondModuleNavKeySerializers]
 * each mix a `data object` and a `data class` key. [FixtureUnregisteredRoute] is deliberately
 * left out of both, to exercise the unregistered-key failure path.
 */
@Serializable
data object FixtureTrainRoot : NavKey

@Serializable
data class FixtureWorkoutDetailsRoute(val workoutId: String, val setIndex: Int?) : NavKey

val fixtureFirstModuleNavKeySerializers = navKeySerializers {
    subclass(FixtureTrainRoot::class, FixtureTrainRoot.serializer())
    subclass(FixtureWorkoutDetailsRoute::class, FixtureWorkoutDetailsRoute.serializer())
}

@Serializable
data object FixtureHistoryRoot : NavKey

@Serializable
data class FixtureHistoryEntryRoute(val workoutId: String) : NavKey

@Serializable
data object FixtureExercisesRoot : NavKey

@Serializable
data object FixtureProfileRoot : NavKey

@Serializable
data object FixtureAuthWelcomeRoute : NavKey

@Serializable
data class FixtureAuthLoginRoute(val prefillEmail: String? = null) : NavKey

val fixtureSecondModuleNavKeySerializers = navKeySerializers {
    subclass(FixtureHistoryRoot::class, FixtureHistoryRoot.serializer())
    subclass(FixtureHistoryEntryRoute::class, FixtureHistoryEntryRoute.serializer())
    subclass(FixtureExercisesRoot::class, FixtureExercisesRoot.serializer())
    subclass(FixtureProfileRoot::class, FixtureProfileRoot.serializer())
    subclass(FixtureAuthWelcomeRoute::class, FixtureAuthWelcomeRoute.serializer())
    subclass(FixtureAuthLoginRoute::class, FixtureAuthLoginRoute.serializer())
}

val fixtureCombinedNavKeySerializers = fixtureFirstModuleNavKeySerializers + fixtureSecondModuleNavKeySerializers

@Serializable
data object FixtureUnregisteredRoute : NavKey
