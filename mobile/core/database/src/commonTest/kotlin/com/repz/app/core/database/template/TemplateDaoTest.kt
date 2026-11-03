package com.repz.app.core.database.template

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
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TemplateDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val templateDao = database.templateDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenSavedTemplate_whenRead_thenAggregateRoundTripsBothNullAndNonNullRepsRanges() =
        runTest {
            val templateId = testUuid("1")
            val openEndedId = testUuid("2")
            val rangedId = testUuid("3")

            templateDao.saveSyncedTemplate(
                template(templateId),
                exercises =
                    listOf(
                        templateExercise(templateId, position = 0, exerciseId = openEndedId, repsMin = null, repsMax = null),
                        templateExercise(templateId, position = 1, exerciseId = rangedId, repsMin = 8, repsMax = 12),
                    ),
            )

            val aggregate = templateDao.findTemplateIncludingDeleted(templateId)
            val exercises = aggregate?.exercises.orEmpty().sortedBy { it.position }

            assertEquals(openEndedId, exercises[0].exerciseId)
            assertNull(exercises[0].targetRepsMin)
            assertNull(exercises[0].targetRepsMax)
            assertEquals(rangedId, exercises[1].exerciseId)
            assertEquals(8, exercises[1].targetRepsMin)
            assertEquals(12, exercises[1].targetRepsMax)
        }

    @Test
    fun givenTemplateSavedTwiceForTheSameId_whenResavedWithFewerExercises_thenNoOrphansRemain() =
        runTest {
            val templateId = testUuid("1")

            templateDao.saveSyncedTemplate(
                template(templateId),
                exercises = listOf(templateExercise(templateId, 0), templateExercise(templateId, 1)),
            )
            templateDao.saveSyncedTemplate(template(templateId), exercises = listOf(templateExercise(templateId, 0)))

            val aggregate = templateDao.findTemplateIncludingDeleted(templateId)
            assertEquals(1, aggregate?.exercises?.size)
        }

    @Test
    fun givenTemplateWithAServerVersion_whenSavedLocally_thenServerVersionIsKeptAndIsDeletedIsForcedFalse() =
        runTest {
            val templateId = testUuid("1")
            templateDao.saveSyncedTemplate(template(templateId, serverVersion = 5), emptyList())

            templateDao.saveLocalTemplate(template(templateId, serverVersion = null, isDeleted = true), emptyList())

            val aggregate = templateDao.findTemplateIncludingDeleted(templateId)
            assertEquals(5L, aggregate?.template?.serverVersion)
            assertEquals(false, aggregate?.template?.isDeleted)
        }

    @Test
    fun givenTemplateSavedLocally_whenSavedSynced_thenServerVersionIsOverwritten() =
        runTest {
            val templateId = testUuid("1")
            templateDao.saveLocalTemplate(template(templateId), emptyList())

            templateDao.saveSyncedTemplate(template(templateId, serverVersion = 7), emptyList())

            assertEquals(7L, templateDao.findTemplateIncludingDeleted(templateId)?.template?.serverVersion)
        }

    @Test
    fun givenATombstonedTemplate_whenObserved_thenHiddenFromObserversButFindIncludingDeletedReturnsItWithChildren() =
        runTest {
            val templateId = testUuid("1")
            templateDao.saveSyncedTemplate(template(templateId), listOf(templateExercise(templateId, 0)))

            templateDao.setDeleted(templateId, isDeleted = true, updatedAt = Instant.parse("2024-01-02T00:00:00Z"))

            templateDao.observeTemplate(templateId).test {
                assertNull(awaitItem())
            }
            val aggregate = templateDao.findTemplateIncludingDeleted(templateId)
            assertEquals(true, aggregate?.template?.isDeleted)
            assertEquals(1, aggregate?.exercises?.size)

            templateDao.deleteChildren(templateId)

            assertTrue(
                templateDao
                    .findTemplateIncludingDeleted(templateId)
                    ?.exercises
                    .orEmpty()
                    .isEmpty(),
            )
        }

    @Test
    fun givenAPurgedTemplate_whenItsChildrenAreCounted_thenNoneRemain() =
        runTest {
            val purgedTemplateId = testUuid("1")
            templateDao.saveSyncedTemplate(template(purgedTemplateId), listOf(templateExercise(purgedTemplateId, 0)))

            templateDao.purge(purgedTemplateId)

            assertNull(templateDao.findTemplateIncludingDeleted(purgedTemplateId))
            assertEquals(0, database.templateRawDao().countExercisesByTemplateId(purgedTemplateId))
        }

    @Test
    fun givenAPurgedTemplatesId_whenReusedByANewTemplateAtTheSamePosition_thenTheInsertSucceedsBecauseCascadeDeletedTheOldChild() =
        runTest {
            val templateId = testUuid("1")
            templateDao.saveSyncedTemplate(template(templateId), listOf(templateExercise(templateId, 0, exerciseId = testUuid("2"))))

            templateDao.purge(templateId)

            // template_exercises' primary key is (template_id, position): if the old row at
            // position 0 had survived the cascade, this second insert at the same id/position
            // would fail on a primary key conflict.
            templateDao.saveSyncedTemplate(template(templateId), listOf(templateExercise(templateId, 0, exerciseId = testUuid("3"))))

            val aggregate = templateDao.findTemplateIncludingDeleted(templateId)
            assertEquals(listOf(testUuid("3")), aggregate?.exercises?.map { it.exerciseId })
        }

    @Test
    fun givenMultipleTemplates_whenObserved_thenOrderedByIdAndTombstonesAreExcluded() =
        runTest {
            val lowerId = testUuid("1")
            val higherId = testUuid("2")
            templateDao.saveSyncedTemplate(template(higherId), emptyList())
            templateDao.saveSyncedTemplate(template(lowerId), emptyList())
            templateDao.saveSyncedTemplate(template(testUuid("3"), isDeleted = true), emptyList())

            templateDao.observeTemplates().test {
                assertEquals(listOf(lowerId, higherId), awaitItem().map { it.template.id })
            }
        }

    private fun template(
        id: Uuid,
        serverVersion: Long? = null,
        isDeleted: Boolean = false,
    ) = WorkoutTemplateEntity(
        id = id,
        name = "Push day",
        notes = "",
        updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
        serverVersion = serverVersion,
        isDeleted = isDeleted,
    )

    private fun templateExercise(
        templateId: Uuid,
        position: Int,
        exerciseId: Uuid = testUuid("999"),
        repsMin: Int? = 5,
        repsMax: Int? = 10,
    ) = TemplateExerciseEntity(
        templateId = templateId,
        position = position,
        exerciseId = exerciseId,
        targetSets = 3,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        restSeconds = 90,
    )
}
