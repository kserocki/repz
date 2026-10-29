package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Each `feature:x:api` exposes one `xNavKeySerializers` built with this; `app:shared` combines
 * them with `+`.
 */
fun navKeySerializers(registerKeys: PolymorphicModuleBuilder<NavKey>.() -> Unit): SerializersModule =
    SerializersModule {
        polymorphic(NavKey::class, builderAction = registerKeys)
    }

/**
 * The [SavedStateConfiguration] the Nav3 Compose layer passes to `rememberSerializable` so every
 * [NavKey] registered in [navKeySerializers] can be saved and restored.
 */
fun navigationSavedStateConfiguration(navKeySerializers: SerializersModule): SavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule = navKeySerializers
    }
