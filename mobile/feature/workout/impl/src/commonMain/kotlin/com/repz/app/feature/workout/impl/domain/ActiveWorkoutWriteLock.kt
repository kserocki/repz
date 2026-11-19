package com.repz.app.feature.workout.impl.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

/**
 * Serializes every write to the active workout draft (ADR-0029): two edits reading the latest
 * Room value and writing it back whole must never race, or the second write would silently discard
 * the first's change.
 */
@Single
class ActiveWorkoutWriteLock {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
