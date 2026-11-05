package com.repz.app.feature.auth.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.repz.app.core.navigation.Navigator
import com.repz.app.feature.auth.api.LoginRoute
import com.repz.app.feature.auth.api.RegisterRoute
import com.repz.app.feature.auth.api.WelcomeRoute
import com.repz.app.feature.auth.impl.presentation.login.LoginScreen
import com.repz.app.feature.auth.impl.presentation.register.RegisterScreen
import com.repz.app.feature.auth.impl.presentation.welcome.WelcomeScreen

/** The signed-out flow's entries. Each screen resolves its own ViewModel and navigates through [navigator] itself (ADR-0005). */
fun EntryProviderScope<NavKey>.authEntries(navigator: Navigator) {
    entry<WelcomeRoute> { WelcomeScreen(navigator) }
    entry<RegisterRoute> { RegisterScreen(navigator) }
    entry<LoginRoute> { LoginScreen(navigator) }
}
