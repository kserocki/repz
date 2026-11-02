package com.repz.app.core.domain.model

/**
 * [name] is the catalog's English name for a catalog exercise, or the user's own name for a
 * custom one. [translations] always includes `"en"` for a catalog exercise and is empty for a
 * custom one; resolve a locale with [localizedName]/[localizedInstructions].
 */
data class Exercise(
    val id: ExerciseId,
    val name: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: Set<Muscle>,
    val equipment: Equipment,
    val origin: ExerciseOrigin,
    val translations: List<ExerciseTranslation>,
) {
    val isCustom: Boolean get() = origin is ExerciseOrigin.Custom
}

/**
 * Attribution always travels with its exercise: it is one synced row, shown together in the About
 * tab, so a custom exercise (no licence) and a catalog exercise without attribution are both
 * unrepresentable.
 */
sealed interface ExerciseOrigin {
    data object Custom : ExerciseOrigin

    data class Catalog(
        val imageUrl: String?,
        val attribution: ExerciseAttribution,
    ) : ExerciseOrigin
}

data class ExerciseAttribution(
    val license: String,
    val author: String,
    val sourceUrl: String,
)

data class ExerciseTranslation(
    val languageTag: String,
    val name: String,
    val instructions: String?,
)
