package com.repz.app.core.database.probe

import androidx.room3.Room
import androidx.room3.withWriteTransaction
import app.cash.turbine.test
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.buildRepzDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

// TODO(REP-24): replace the probe with the real schema.
class ProbeDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val probeDao = database.probeDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenEmptyDatabase_whenProbeUpserted_thenGetByIdReturnsIt() =
        runTest {
            val probe = ProbeEntity(id = "probe-1", label = "first probe")

            probeDao.upsert(probe)

            assertEquals(probe, probeDao.getById(probe.id))
        }

    @Test
    fun givenObservedProbes_whenProbeUpserted_thenFlowEmitsUpdatedList() =
        runTest {
            probeDao.observeAll().test {
                assertEquals(emptyList(), awaitItem())

                val probe = ProbeEntity(id = "probe-1", label = "first probe")
                probeDao.upsert(probe)

                assertEquals(listOf(probe), awaitItem())
            }
        }

    @Test
    fun givenWriteTransaction_whenBlockThrowsAfterUpsert_thenNothingIsPersisted() =
        runTest {
            val probe = ProbeEntity(id = "probe-1", label = "first probe")

            assertFailsWith<RollbackProbeException> {
                database.withWriteTransaction {
                    probeDao.upsert(probe)
                    throw RollbackProbeException()
                }
            }

            assertNull(probeDao.getById(probe.id))
        }

    private class RollbackProbeException : Exception()
}
