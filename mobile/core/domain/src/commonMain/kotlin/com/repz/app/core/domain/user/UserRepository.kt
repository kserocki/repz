package com.repz.app.core.domain.user

import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow

/** The signed-in user's profile (rule 6: the UI reads only from Room). */
interface UserRepository {
    /** The signed-in user's profile as stored on this device; null until it is known. */
    fun observeCurrentUser(): Flow<User?>

    /** Fetches the profile from the server and stores it; on failure the stored copy stays. */
    suspend fun refreshCurrentUser(): DomainResult<User, UserRefreshError>
}
