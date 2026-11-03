package com.repz.app.feature.history.api

import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.navKeySerializers
import kotlinx.serialization.Serializable

/** The History tab's root. */
@Serializable
data object HistoryRoute : NavKey

val historyNavKeySerializers =
    navKeySerializers {
        subclass(HistoryRoute::class, HistoryRoute.serializer())
    }
