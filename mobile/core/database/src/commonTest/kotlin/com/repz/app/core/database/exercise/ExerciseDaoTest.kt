package com.repz.app.core.database.exercise

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

class ExerciseDaoTest {
    // Room needs a real dispatcher for its query/invalidation work, not the runTest scheduler.
    private val database =
        Room.inMemoryDatabaseBuilder<RepzDatabase>().buildRepzDatabase(Dispatchers.IO)
    private val exerciseDao = database.exerciseDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenACatalogExercise_whenSaved_thenAttributionAndEnglishTranslationRoundTrip() =
        runTest {
            val id = testUuid("1")
            exerciseDao.saveSyncedExercise(
                exercise(
                    id,
                    isCustom = false,
                    imageUrl = "https://example.com/bench.webp",
                    license = "CC BY-SA 4.0",
                    author = "wger",
                    sourceUrl = "https://wger.de/exercise/1",
                    secondaryMuscles = listOf("triceps", "shoulders"),
                ),
                translations = listOf(ExerciseTranslationEntity(id, "en", "Bench press", "Lie on the bench.")),
            )

            val result = exerciseDao.findExercisesIncludingDeleted(listOf(id)).single()

            assertEquals(false, result.exercise.isCustom)
            assertEquals("https://example.com/bench.webp", result.exercise.imageUrl)
            assertEquals("CC BY-SA 4.0", result.exercise.license)
            assertEquals("wger", result.exercise.author)
            assertEquals("https://wger.de/exercise/1", result.exercise.sourceUrl)
            assertEquals(listOf("triceps", "shoulders"), result.exercise.secondaryMuscles)
            assertEquals(listOf("en"), result.translations.map { it.locale })
        }

    @Test
    fun givenACustomExercise_whenSaved_thenHasNoAttributionAndNoTranslationsAndEmptySecondaryMuscles() =
        runTest {
            val id = testUuid("1")
            exerciseDao.saveSyncedExercise(
                exercise(
                    id,
                    isCustom = true,
                    imageUrl = null,
                    license = null,
                    author = null,
                    sourceUrl = null,
                    secondaryMuscles = emptyList(),
                ),
                translations = emptyList(),
            )

            val result = exerciseDao.findExercisesIncludingDeleted(listOf(id)).single()

            assertEquals(true, result.exercise.isCustom)
            assertNull(result.exercise.imageUrl)
            assertTrue(result.exercise.secondaryMuscles.isEmpty())
            assertTrue(result.translations.isEmpty())
        }

    @Test
    fun givenASingleSecondaryMuscle_whenSaved_thenRoundTripsAsOneValue() =
        runTest {
            val id = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(id, secondaryMuscles = listOf("biceps")), emptyList())

            assertEquals(
                listOf("biceps"),
                exerciseDao
                    .findExercisesIncludingDeleted(listOf(id))
                    .single()
                    .exercise.secondaryMuscles,
            )
        }

    @Test
    fun givenAnExerciseWithTranslations_whenResaved_thenOldTranslationsAreReplaced() =
        runTest {
            val id = testUuid("1")
            exerciseDao.saveSyncedExercise(
                exercise(id),
                translations =
                    listOf(
                        ExerciseTranslationEntity(id, "en", "Bench press", null),
                        ExerciseTranslationEntity(id, "pl", "Wyciskanie", null),
                    ),
            )

            exerciseDao.saveSyncedExercise(exercise(id), translations = listOf(ExerciseTranslationEntity(id, "en", "Bench press", null)))

            val result = exerciseDao.findExercisesIncludingDeleted(listOf(id)).single()
            assertEquals(listOf("en"), result.translations.map { it.locale })
        }

    @Test
    fun givenAnExerciseWithAServerVersion_whenSavedLocally_thenServerVersionIsKeptAndIsDeletedIsForcedFalse() =
        runTest {
            val id = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(id, serverVersion = 5), emptyList())

            exerciseDao.saveLocalExercise(exercise(id, serverVersion = null, isDeleted = true), emptyList())

            val result = exerciseDao.findExercisesIncludingDeleted(listOf(id)).single()
            assertEquals(5L, result.exercise.serverVersion)
            assertEquals(false, result.exercise.isDeleted)
        }

    @Test
    fun givenATombstonedExercise_whenObserved_thenHiddenFromObserveExercisesButFindIncludingDeletedReturnsIt() =
        runTest {
            val id = testUuid("1")
            exerciseDao.saveSyncedExercise(exercise(id), emptyList())

            exerciseDao.setDeleted(id, isDeleted = true, updatedAt = Instant.parse("2024-01-02T00:00:00Z"))

            exerciseDao.observeExercises().test {
                assertTrue(awaitItem().isEmpty())
            }
            assertEquals(
                true,
                exerciseDao
                    .findExercisesIncludingDeleted(listOf(id))
                    .single()
                    .exercise.isDeleted,
            )
        }

    private fun exercise(
        id: Uuid,
        isCustom: Boolean = false,
        imageUrl: String? = null,
        license: String? = null,
        author: String? = null,
        sourceUrl: String? = null,
        secondaryMuscles: List<String> = emptyList(),
        serverVersion: Long? = null,
        isDeleted: Boolean = false,
    ) = ExerciseEntity(
        id = id,
        name = "Bench press",
        primaryMuscle = "chest",
        secondaryMuscles = secondaryMuscles,
        equipment = "barbell",
        isCustom = isCustom,
        imageUrl = imageUrl,
        license = license,
        author = author,
        sourceUrl = sourceUrl,
        updatedAt = Instant.parse("2024-01-01T11:00:00Z"),
        serverVersion = serverVersion,
        isDeleted = isDeleted,
    )
}
