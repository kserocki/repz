package com.repz.app.core.database.account

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
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class CurrentUserDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val currentUserDao = database.currentUserDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAnEmptyDatabase_whenObserved_thenEmitsNull() =
        runTest {
            currentUserDao.observe().test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun givenNoRow_whenReplaced_thenTheRowIsStored() =
        runTest {
            val entity = userEntity(testUuid("1"), "alex@repz.app")

            currentUserDao.replace(entity)

            currentUserDao.observe().test {
                assertEquals(entity, awaitItem())
            }
        }

    @Test
    fun givenAStoredRow_whenReplacedWithAnotherAccount_thenOnlyTheNewAccountsRowRemains() =
        runTest {
            currentUserDao.replace(userEntity(testUuid("1"), "alex@repz.app"))

            val secondAccount = userEntity(testUuid("2"), "sam@repz.app")
            currentUserDao.replace(secondAccount)

            currentUserDao.observe().test {
                assertEquals(secondAccount, awaitItem())
            }
        }

    @Test
    fun givenAStoredRow_whenCleared_thenObserveEmitsNull() =
        runTest {
            currentUserDao.replace(userEntity(testUuid("1"), "alex@repz.app"))

            currentUserDao.clear()

            currentUserDao.observe().test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun givenAnEmptyDatabase_whenObservedThroughoutReplaceAndClear_thenEmitsNullThenTheRowThenNull() =
        runTest {
            currentUserDao.observe().test {
                assertNull(awaitItem())

                val entity = userEntity(testUuid("1"), "alex@repz.app")
                currentUserDao.replace(entity)
                assertEquals(entity, awaitItem())

                currentUserDao.clear()
                assertNull(awaitItem())
            }
        }

    private fun userEntity(
        id: Uuid,
        email: String,
    ) = CurrentUserEntity(id = id, email = email, displayName = "Alex", unitSystem = "kg")
}
