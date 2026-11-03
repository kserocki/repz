package com.repz.app.feature.auth.api

import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.navKeySerializers
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute : NavKey

@Serializable
data object RegisterRoute : NavKey

@Serializable
data object LoginRoute : NavKey

val authNavKeySerializers =
    navKeySerializers {
        subclass(WelcomeRoute::class, WelcomeRoute.serializer())
        subclass(RegisterRoute::class, RegisterRoute.serializer())
        subclass(LoginRoute::class, LoginRoute.serializer())
    }
