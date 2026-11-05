package com.repz.app.feature.settings.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.repz.app.feature.settings.api.ProfileRoute
import com.repz.app.feature.settings.impl.presentation.profile.ProfileScreen

/** No `navigator` parameter yet: Profile has nothing to navigate to until a later issue adds sub-screens. */
fun EntryProviderScope<NavKey>.settingsEntries() {
    entry<ProfileRoute> { ProfileScreen() }
}
