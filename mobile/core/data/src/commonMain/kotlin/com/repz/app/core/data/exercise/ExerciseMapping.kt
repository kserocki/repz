package com.repz.app.core.data.exercise

import co.touchlab.kermit.Logger
import com.repz.app.core.database.exercise.ExerciseEntity
import com.repz.app.core.database.exercise.ExerciseTranslationEntity
import com.repz.app.core.database.exercise.ExerciseWithTranslations
import com.repz.app.core.domain.model.Equipment
import com.repz.app.core.domain.model.Exercise
import com.repz.app.core.domain.model.ExerciseAttribution
import com.repz.app.core.domain.model.ExerciseId
import com.repz.app.core.domain.model.ExerciseOrigin
import com.repz.app.core.domain.model.ExerciseTranslation
import com.repz.app.core.domain.model.Muscle
import kotlin.time.Instant

/**
 * A corrupt row (R5: unknown vocabulary text, or a catalog row without attribution) is logged and
 * dropped. Each field maps independently first, so every problem with a row is logged, not just
 * the first one; the nested `let`s below only decide whether the whole row is still buildable.
 */
internal fun ExerciseWithTranslations.toDomainOrNull(logger: Logger): Exercise? {
    val primaryMuscle = exercise.primaryMuscle.toMuscleOrNull(logger)
    val secondaryMuscles = exercise.secondaryMuscles.toMuscleSetOrNull(logger)
    val equipment = exercise.equipment.toEquipmentOrNull(logger)
    val origin = exercise.toOriginOrNull(logger)

    return primaryMuscle?.let { validPrimaryMuscle ->
        secondaryMuscles?.let { validSecondaryMuscles ->
            equipment?.let { validEquipment ->
                origin?.let { validOrigin ->
                    Exercise(
                        id = ExerciseId(exercise.id),
                        name = exercise.name,
                        primaryMuscle = validPrimaryMuscle,
                        secondaryMuscles = validSecondaryMuscles,
                        equipment = validEquipment,
                        origin = validOrigin,
                        translations = translations.map { it.toDomain() },
                    )
                }
            }
        }
    }
}

internal fun List<ExerciseWithTranslations>.toDomainList(logger: Logger): List<Exercise> = mapNotNull { it.toDomainOrNull(logger) }

private fun ExerciseEntity.toOriginOrNull(logger: Logger): ExerciseOrigin? {
    // Local vals, not the entity's own properties: a cross-module property can't be smart-cast.
    val license = license
    val author = author
    val sourceUrl = sourceUrl
    return if (isCustom) {
        ExerciseOrigin.Custom
    } else if (license != null && author != null && sourceUrl != null) {
        ExerciseOrigin.Catalog(imageUrl, ExerciseAttribution(license, author, sourceUrl))
    } else {
        logger.e { "Catalog exercise $id is missing attribution" }
        null
    }
}

private fun ExerciseTranslationEntity.toDomain(): ExerciseTranslation =
    ExerciseTranslation(languageTag = locale, name = name, instructions = instructions)

private fun List<String>.toMuscleSetOrNull(logger: Logger): Set<Muscle>? {
    val mapped = map { it.toMuscleOrNull(logger) }
    return if (mapped.any { it == null }) null else mapped.filterNotNull().toSet()
}

private fun String.toMuscleOrNull(logger: Logger): Muscle? =
    when (this) {
        "chest" -> {
            Muscle.CHEST
        }

        "back" -> {
            Muscle.BACK
        }

        "shoulders" -> {
            Muscle.SHOULDERS
        }

        "biceps" -> {
            Muscle.BICEPS
        }

        "triceps" -> {
            Muscle.TRICEPS
        }

        "forearms" -> {
            Muscle.FOREARMS
        }

        "abs" -> {
            Muscle.ABS
        }

        "glutes" -> {
            Muscle.GLUTES
        }

        "quadriceps" -> {
            Muscle.QUADRICEPS
        }

        "hamstrings" -> {
            Muscle.HAMSTRINGS
        }

        "calves" -> {
            Muscle.CALVES
        }

        "full_body" -> {
            Muscle.FULL_BODY
        }

        else -> {
            logger.e { "Unknown exercises.muscle value: $this" }
            null
        }
    }

private fun Muscle.toWireText(): String =
    when (this) {
        Muscle.CHEST -> "chest"
        Muscle.BACK -> "back"
        Muscle.SHOULDERS -> "shoulders"
        Muscle.BICEPS -> "biceps"
        Muscle.TRICEPS -> "triceps"
        Muscle.FOREARMS -> "forearms"
        Muscle.ABS -> "abs"
        Muscle.GLUTES -> "glutes"
        Muscle.QUADRICEPS -> "quadriceps"
        Muscle.HAMSTRINGS -> "hamstrings"
        Muscle.CALVES -> "calves"
        Muscle.FULL_BODY -> "full_body"
    }

private fun String.toEquipmentOrNull(logger: Logger): Equipment? =
    when (this) {
        "barbell" -> {
            Equipment.BARBELL
        }

        "dumbbell" -> {
            Equipment.DUMBBELL
        }

        "kettlebell" -> {
            Equipment.KETTLEBELL
        }

        "machine" -> {
            Equipment.MACHINE
        }

        "cable" -> {
            Equipment.CABLE
        }

        "bodyweight" -> {
            Equipment.BODYWEIGHT
        }

        "band" -> {
            Equipment.BAND
        }

        "other" -> {
            Equipment.OTHER
        }

        else -> {
            logger.e { "Unknown exercises.equipment value: $this" }
            null
        }
    }

private fun Equipment.toWireText(): String =
    when (this) {
        Equipment.BARBELL -> "barbell"
        Equipment.DUMBBELL -> "dumbbell"
        Equipment.KETTLEBELL -> "kettlebell"
        Equipment.MACHINE -> "machine"
        Equipment.CABLE -> "cable"
        Equipment.BODYWEIGHT -> "bodyweight"
        Equipment.BAND -> "band"
        Equipment.OTHER -> "other"
    }

/** [serverVersion]/[isDeleted] are placeholders: [com.repz.app.core.database.exercise.ExerciseDao.saveLocalExercise] overwrites both. */
internal fun Exercise.toEntity(updatedAt: Instant): ExerciseEntity {
    val origin = this.origin
    return ExerciseEntity(
        id = id.value,
        name = name,
        primaryMuscle = primaryMuscle.toWireText(),
        // Written in the server's own vocabulary order (ADR-0019), not insertion order.
        secondaryMuscles = secondaryMuscles.sortedBy { it.ordinal }.map { it.toWireText() },
        equipment = equipment.toWireText(),
        isCustom = origin is ExerciseOrigin.Custom,
        imageUrl = (origin as? ExerciseOrigin.Catalog)?.imageUrl,
        license = (origin as? ExerciseOrigin.Catalog)?.attribution?.license,
        author = (origin as? ExerciseOrigin.Catalog)?.attribution?.author,
        sourceUrl = (origin as? ExerciseOrigin.Catalog)?.attribution?.sourceUrl,
        updatedAt = updatedAt,
        serverVersion = null,
        isDeleted = false,
    )
}

internal fun Exercise.toTranslationEntities(): List<ExerciseTranslationEntity> =
    translations.map {
        ExerciseTranslationEntity(exerciseId = id.value, locale = it.languageTag, name = it.name, instructions = it.instructions)
    }
