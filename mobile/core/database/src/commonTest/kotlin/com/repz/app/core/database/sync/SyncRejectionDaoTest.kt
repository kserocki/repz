package com.repz.app.core.database.sync

import androidx.room3.Room
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import com.repz.app.core.database.testUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncRejectionDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val syncRejectionDao = database.syncRejectionDao()
    private val entityId = testUuid("1")

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenNoRejection_whenUpserted_thenItIsStored() =
        runTest {
            val rejection = SyncRejectionEntity(SyncEntityType.WORKOUT, entityId, "validation-error", Instant.parse("2024-01-01T00:00:00Z"))

            syncRejectionDao.upsert(rejection)

            syncRejectionDao.observeAll().test {
                assertEquals(listOf(rejection), awaitItem())
            }
        }

    @Test
    fun givenARejection_whenUpsertedAgainForTheSameEntity_thenTheStoredRowIsReplacedNotDuplicated() =
        runTest {
            syncRejectionDao.upsert(
                SyncRejectionEntity(SyncEntityType.WORKOUT, entityId, "validation-error", Instant.parse("2024-01-01T00:00:00Z")),
            )

            syncRejectionDao.upsert(
                SyncRejectionEntity(SyncEntityType.WORKOUT, entityId, "id-conflict", Instant.parse("2024-01-02T00:00:00Z")),
            )

            syncRejectionDao.observeAll().test {
                val rejections = awaitItem()
                assertEquals(1, rejections.size)
                assertEquals("id-conflict", rejections.single().problemType)
            }
        }

    @Test
    fun givenARejection_whenDeleted_thenItIsRemoved() =
        runTest {
            syncRejectionDao.upsert(
                SyncRejectionEntity(SyncEntityType.WORKOUT, entityId, "validation-error", Instant.parse("2024-01-01T00:00:00Z")),
            )

            syncRejectionDao.delete(SyncEntityType.WORKOUT, entityId)

            syncRejectionDao.observeAll().test {
                assertEquals(emptyList(), awaitItem())
            }
        }

    @Test
    fun givenRejections_whenObservingCount_thenEmitsEachChange() =
        runTest {
            syncRejectionDao.observeCount().test {
                assertEquals(0, awaitItem())
                syncRejectionDao.upsert(
                    SyncRejectionEntity(SyncEntityType.WORKOUT, entityId, "validation-error", Instant.parse("2024-01-01T00:00:00Z")),
                )
                assertEquals(1, awaitItem())
            }
        }
}
