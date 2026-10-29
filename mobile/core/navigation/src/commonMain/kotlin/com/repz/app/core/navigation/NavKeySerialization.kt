package com.repz.app.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Builds a [SerializersModule] that registers one group of [NavKey] subclasses for polymorphic
 * serialization. Each `feature:x:api` module exposes one `val xNavKeySerializers` built with
 * this function; `app:shared` combines every module's serializers with `+` before passing them
 * to [navigationSavedStateConfiguration]. `core:navigation` cannot see `feature:*:api` (that
 * would be a cycle), so registration lives with the keys, not here.
 */
fun navKeySerializers(registerKeys: PolymorphicModuleBuilder<NavKey>.() -> Unit): SerializersModule =
    SerializersModule {
        polymorphic(NavKey::class, builderAction = registerKeys)
    }

/**
 * The [SavedStateConfiguration] the Nav3 Compose layer passes to `rememberSerializable` and
 * `rememberNavBackStack` so every [NavKey] registered in [navKeySerializers] can be saved and
 * restored through `androidx.savedstate`.
 */
fun navigationSavedStateConfiguration(navKeySerializers: SerializersModule): SavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule = navKeySerializers
    }
