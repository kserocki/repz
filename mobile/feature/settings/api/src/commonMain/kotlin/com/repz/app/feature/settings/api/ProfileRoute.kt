package com.repz.app.feature.settings.api

import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.navKeySerializers
import kotlinx.serialization.Serializable

/** The Profile tab's root. */
@Serializable
data object ProfileRoute : NavKey

val settingsNavKeySerializers =
    navKeySerializers {
        subclass(ProfileRoute::class, ProfileRoute.serializer())
    }
