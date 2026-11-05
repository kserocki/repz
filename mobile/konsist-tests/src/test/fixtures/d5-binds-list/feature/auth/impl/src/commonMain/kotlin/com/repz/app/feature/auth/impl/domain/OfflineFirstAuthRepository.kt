package com.repz.app.feature.auth.impl.domain

import org.koin.core.annotation.Single

// Declares its binds explicitly instead of relying on auto-binding - D5 must still parse the
// `binds = [...]` list and catch the clash with the explicit provider in LegacyAuthModule.kt.
@Single(binds = [AuthRepository::class])
class OfflineFirstAuthRepository : AuthRepository
