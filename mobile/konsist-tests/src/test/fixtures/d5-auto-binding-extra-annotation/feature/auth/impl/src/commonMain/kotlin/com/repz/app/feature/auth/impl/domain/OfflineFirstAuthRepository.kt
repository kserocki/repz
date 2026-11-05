package com.repz.app.feature.auth.impl.domain

import androidx.compose.runtime.Stable
import org.koin.core.annotation.Single

// An unrelated annotation (not one of Koin's own qualifier annotations) must never change D5's
// qualifier key, or this auto-bound definition would wrongly look like a different owner than the
// explicit provider in LegacyAuthModule.kt for the same AuthRepository.
@Single
@Stable
class OfflineFirstAuthRepository : AuthRepository
