package com.repz.app.feature.auth.impl.domain

import org.koin.core.annotation.Single

// Auto-binds to AuthRepository (its one direct interface) - D5 must count that as an owner of
// AuthRepository, not just of its own class name, or it would miss the clash with the explicit
// provider in LegacyAuthModule.kt.
@Single
class OfflineFirstAuthRepository : AuthRepository
