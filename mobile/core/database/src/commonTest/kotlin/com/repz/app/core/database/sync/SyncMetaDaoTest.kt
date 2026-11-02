package com.repz.app.core.database.sync

import androidx.room3.Room
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncMetaDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val syncMetaDao = database.syncMetaDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAnEmptyDatabase_whenRead_thenReturnsInitial() =
        runTest {
            assertEquals(SyncMetaEntity.INITIAL, syncMetaDao.get())
        }

    @Test
    fun givenNoRow_whenSavingTheCursor_thenInsertsIt() =
        runTest {
            val meta = SyncMetaEntity.INITIAL.copy(pullCursor = 500)

            syncMetaDao.save(meta)

            assertEquals(meta, syncMetaDao.get())
        }

    @Test
    fun givenAStoredRow_whenSavedAgain_thenTheSingletonRowIsUpdatedNotDuplicated() =
        runTest {
            syncMetaDao.save(SyncMetaEntity.INITIAL.copy(pullCursor = 500))

            syncMetaDao.save(SyncMetaEntity.INITIAL.copy(pullCursor = 1_000))

            assertEquals(1_000L, syncMetaDao.get().pullCursor)
        }

    @Test
    fun givenSavedMeta_whenObserved_thenEmitsInitialThenTheSavedValue() =
        runTest {
            syncMetaDao.observe().test {
                assertEquals(SyncMetaEntity.INITIAL, awaitItem())

                val meta = SyncMetaEntity.INITIAL.copy(pullCursor = 42, lastSuccessfulSyncAt = Instant.parse("2024-01-01T00:00:00Z"))
                syncMetaDao.save(meta)

                assertEquals(meta, awaitItem())
            }
        }
}
