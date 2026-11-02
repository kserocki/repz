package com.repz.app.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

private fun testExercise(
    translations: List<ExerciseTranslation>,
    origin: ExerciseOrigin = ExerciseOrigin.Custom,
): Exercise =
    Exercise(
        id = ExerciseId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
        name = "Bench press",
        primaryMuscle = Muscle.CHEST,
        secondaryMuscles = setOf(Muscle.TRICEPS),
        equipment = Equipment.BARBELL,
        origin = origin,
        translations = translations,
    )

private fun catalogAttribution() =
    ExerciseAttribution(license = "CC BY-SA 4.0", author = "wger.de", sourceUrl = "https://wger.de/exercise/1")

class ExerciseLocalizationTest {
    @Test
    fun givenAnExactTagMatch_whenLocalized_thenItsTranslationWins() {
        val exercise =
            testExercise(
                listOf(
                    ExerciseTranslation("en", "Bench press", "Lie on a bench."),
                    ExerciseTranslation("de", "Bankdrücken", "Auf eine Bank legen."),
                ),
            )

        assertEquals("Bankdrücken", exercise.localizedName("de"))
        assertEquals("Auf eine Bank legen.", exercise.localizedInstructions("de"))
    }

    @Test
    fun givenATagInADifferentCase_whenLocalized_thenItStillMatchesExactly() {
        val exercise = testExercise(listOf(ExerciseTranslation("de", "Bankdrücken", "Auf eine Bank legen.")))

        assertEquals("Bankdrücken", exercise.localizedName("DE"))
    }

    @Test
    fun givenNoExactTagButTheSamePrimaryLanguage_whenLocalized_thenThatTranslationWins() {
        val exercise = testExercise(listOf(ExerciseTranslation("pl", "Wyciskanie leżąc", "Połóż się na ławce.")))

        assertEquals("Wyciskanie leżąc", exercise.localizedName("pl-PL"))
    }

    @Test
    fun givenADifferentRegionOfTheSamePrimaryLanguage_whenLocalized_thenThatTranslationWins() {
        val exercise = testExercise(listOf(ExerciseTranslation("pt-BR", "Supino reto", "Deite-se no banco.")))

        assertEquals("Supino reto", exercise.localizedName("pt-PT"))
    }

    @Test
    fun givenEnglishListedBeforeTheSamePrimaryLanguage_whenLocalized_thenThePrimaryLanguageStillWins() {
        val exercise =
            testExercise(
                listOf(
                    ExerciseTranslation("en", "Bench press", "Lie on a bench."),
                    ExerciseTranslation("pl", "Wyciskanie leżąc", "Połóż się na ławce."),
                ),
            )

        assertEquals("Wyciskanie leżąc", exercise.localizedName("pl-PL"))
    }

    @Test
    fun givenASiblingRegionListedBeforeTheExactTag_whenLocalized_thenTheExactTagStillWins() {
        val exercise =
            testExercise(
                listOf(
                    ExerciseTranslation("pt-BR", "Supino reto", "Deite-se no banco."),
                    ExerciseTranslation("pt-PT", "Supino plano", "Deite-se no banco plano."),
                ),
            )

        assertEquals("Supino plano", exercise.localizedName("pt-PT"))
    }

    @Test
    fun givenAnUnderscoreSeparatedDeviceTag_whenLocalized_thenItNormalizesToHyphenated() {
        val exercise = testExercise(listOf(ExerciseTranslation("pl", "Wyciskanie leżąc", "Połóż się na ławce.")))

        assertEquals("Wyciskanie leżąc", exercise.localizedName("pl_PL"))
    }

    @Test
    fun givenNoMatchingLanguage_whenLocalized_thenEnglishWins() {
        val exercise =
            testExercise(
                listOf(
                    ExerciseTranslation("en", "Bench press", "Lie on a bench."),
                    ExerciseTranslation("de", "Bankdrücken", "Auf eine Bank legen."),
                ),
            )

        assertEquals("Bench press", exercise.localizedName("fr"))
        assertEquals("Lie on a bench.", exercise.localizedInstructions("fr"))
    }

    @Test
    fun givenACustomExerciseWithNoTranslations_whenLocalized_thenItsOwnNameWins() {
        val exercise = testExercise(translations = emptyList())

        assertEquals("Bench press", exercise.localizedName("de"))
    }

    @Test
    fun givenACustomExerciseWithNoTranslations_whenInstructionsLocalized_thenTheyAreNull() {
        val exercise = testExercise(translations = emptyList())

        assertEquals(null, exercise.localizedInstructions("de"))
    }

    @Test
    fun givenTheDeviceLocaleHasANameButNoInstructions_whenInstructionsLocalized_thenEnglishWins() {
        val exercise =
            testExercise(
                listOf(
                    ExerciseTranslation("en", "Bench press", "Lie on a bench."),
                    ExerciseTranslation("de", "Bankdrücken", instructions = null),
                ),
            )

        assertEquals("Bankdrücken", exercise.localizedName("de"))
        assertEquals("Lie on a bench.", exercise.localizedInstructions("de"))
    }

    @Test
    fun givenACustomOrigin_whenIsCustomChecked_thenItIsTrue() {
        val exercise = testExercise(translations = emptyList(), origin = ExerciseOrigin.Custom)

        assertTrue(exercise.isCustom)
    }

    @Test
    fun givenACatalogOrigin_whenIsCustomChecked_thenItIsFalse() {
        val exercise =
            testExercise(
                translations = listOf(ExerciseTranslation("en", "Bench press", "Lie on a bench.")),
                origin = ExerciseOrigin.Catalog(imageUrl = null, attribution = catalogAttribution()),
            )

        assertFalse(exercise.isCustom)
    }
}
