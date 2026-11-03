package com.repz.app.core.data

import com.repz.app.core.data.session.OfflineFirstSessionRepository
import com.repz.app.core.domain.session.SessionRepository
import com.repz.app.core.network.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** `core:data`'s one `@Module` (ARCH §9, konsist D3). Later repositories are added here too. */
@Module
class DataModule {
    @Single
    fun sessionRepository(
        sessionManager: SessionManager,
        applicationScope: CoroutineScope,
    ): SessionRepository = OfflineFirstSessionRepository(sessionManager, applicationScope)
}
