package com.repz.app.core.domain.exercise

import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.ExerciseOrigin
import com.repz.app.core.domain.model.ExerciseTranslation
import com.repz.app.core.domain.model.Muscle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private fun testExercise(
    seed: Int,
    name: String,
    primaryMuscle: Muscle = Muscle.CHEST,
    secondaryMuscles: Set<Muscle> = emptySet(),
    equipment: Equipment = Equipment.BARBELL,
    translations: List<ExerciseTranslation> = listOf(ExerciseTranslation("en", name, null)),
): Exercise =
    Exercise(
        id = ExerciseId(Uuid.parse("00000000-0000-0000-0000-" + seed.toString().padStart(12, '0'))),
        name = name,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
        origin = ExerciseOrigin.Custom,
        translations = translations,
    )

class ExerciseSearchTest {
    @Test
    fun givenAMultiWordQuery_whenSearched_thenEveryTokenMustMatch() {
        val benchPress = testExercise(1, "Bench press")
        val legPress = testExercise(2, "Leg press")

        val result = listOf(benchPress, legPress).searchedBy("bench press", "en")

        assertEquals(listOf(benchPress), result)
    }

    @Test
    fun givenATokenMatchingOnlyTheEnglishName_whenSearchedUnderAnotherLocale_thenItStillMatches() {
        val exercise = testExercise(1, "Bench press", translations = listOf(ExerciseTranslation("pl", "Wyciskanie sztangi", null)))

        val result = listOf(exercise).searchedBy("bench", "pl")

        assertEquals(listOf(exercise), result)
    }

    @Test
    fun givenAnUppercaseQuery_whenSearched_thenItMatchesCaseInsensitively() {
        val exercise = testExercise(1, "Bench press")

        val result = listOf(exercise).searchedBy("BENCH", "en")

        assertEquals(listOf(exercise), result)
    }

    @Test
    fun givenALocalizedNameStartingWithTheQuery_whenRanked_thenItSortsBeforeAWordMatch() {
        val prefixMatch = testExercise(1, "Bench press")
        val wordMatch = testExercise(2, "Incline bench press")

        val result = listOf(wordMatch, prefixMatch).searchedBy("bench", "en")

        assertEquals(listOf(prefixMatch, wordMatch), result)
    }

    @Test
    fun givenAWordStartingWithTheToken_whenRanked_thenItSortsBeforeASubstringOnlyMatch() {
        val wordMatch = testExercise(1, "Incline bench press")
        val substringOnly = testExercise(2, "Overbench press")

        val result = listOf(substringOnly, wordMatch).searchedBy("bench", "en")

        assertEquals(listOf(wordMatch, substringOnly), result)
    }

    @Test
    fun givenAHyphenatedName_whenRanked_thenTheWordAfterTheHyphenCountsAsItsOwnWord() {
        val hyphenWordMatch = testExercise(1, "Push-bench press")
        val substringOnly = testExercise(2, "Overbench press")

        val result = listOf(substringOnly, hyphenWordMatch).searchedBy("bench", "en")

        assertEquals(listOf(hyphenWordMatch, substringOnly), result)
    }

    @Test
    fun givenTwoMatchesWithTheSameRank_whenSorted_thenTheyOrderByLocalizedName() {
        val b = testExercise(2, "Bench press B")
        val a = testExercise(1, "Bench press A")

        val result = listOf(b, a).searchedBy("bench", "en")

        assertEquals(listOf(a, b), result)
    }

    @Test
    fun givenTwoMatchesWithTheSameRankAndName_whenSorted_thenIdBreaksTheTie() {
        val higherId = testExercise(2, "Bench press")
        val lowerId = testExercise(1, "Bench press")

        val result = listOf(higherId, lowerId).searchedBy("bench", "en")

        assertEquals(listOf(lowerId, higherId), result)
    }

    @Test
    fun givenABlankQuery_whenSearched_thenEveryExerciseIsKeptSortedByLocalizedNameThenId() {
        val squat = testExercise(2, "Squat")
        val benchPress = testExercise(1, "Bench press")

        val result = listOf(squat, benchPress).searchedBy("   ", "en")

        assertEquals(listOf(benchPress, squat), result)
    }

    @Test
    fun givenAMuscleFilter_whenMatched_thenPrimaryOrAnySecondaryMuscleCounts() {
        val primaryMatch = testExercise(1, "Bench press", primaryMuscle = Muscle.CHEST)
        val secondaryMatch = testExercise(2, "Dip", primaryMuscle = Muscle.TRICEPS, secondaryMuscles = setOf(Muscle.CHEST))
        val noMatch = testExercise(3, "Row", primaryMuscle = Muscle.BACK)
        val filter = ExerciseFilter(muscles = setOf(Muscle.CHEST))

        assertTrue(filter.matches(primaryMatch))
        assertTrue(filter.matches(secondaryMatch))
        assertFalse(filter.matches(noMatch))
    }

    @Test
    fun givenMultipleMusclesInTheFilter_whenMatched_thenTheyCombineWithOr() {
        val chest = testExercise(1, "Bench press", primaryMuscle = Muscle.CHEST)
        val back = testExercise(2, "Row", primaryMuscle = Muscle.BACK)
        val legs = testExercise(3, "Squat", primaryMuscle = Muscle.QUADRICEPS)
        val filter = ExerciseFilter(muscles = setOf(Muscle.CHEST, Muscle.BACK))

        assertTrue(filter.matches(chest))
        assertTrue(filter.matches(back))
        assertFalse(filter.matches(legs))
    }

    @Test
    fun givenAMuscleAndAnEquipmentFilter_whenMatched_thenTheyCombineWithAnd() {
        val both = testExercise(1, "Bench press", primaryMuscle = Muscle.CHEST, equipment = Equipment.BARBELL)
        val muscleOnly = testExercise(2, "Dumbbell fly", primaryMuscle = Muscle.CHEST, equipment = Equipment.DUMBBELL)
        val filter = ExerciseFilter(muscles = setOf(Muscle.CHEST), equipment = setOf(Equipment.BARBELL))

        assertTrue(filter.matches(both))
        assertFalse(filter.matches(muscleOnly))
    }

    @Test
    fun givenAnEmptyFilter_whenMatched_thenEveryExerciseMatchesAndItIsNotActive() {
        val exercise = testExercise(1, "Bench press")
        val filter = ExerciseFilter()

        assertTrue(filter.matches(exercise))
        assertFalse(filter.isActive)
        assertEquals(0, filter.activeCount)
    }

    @Test
    fun givenAnActiveFilter_whenCounted_thenActiveCountSumsBothGroups() {
        val filter = ExerciseFilter(muscles = setOf(Muscle.CHEST, Muscle.BACK), equipment = setOf(Equipment.BARBELL))

        assertEquals(3, filter.activeCount)
        assertTrue(filter.isActive)
    }
}
