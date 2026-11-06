package com.repz.app.core.domain.template

import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import kotlinx.coroutines.flow.Flow

/**
 * Workout templates (rule 6: the UI reads only from Room). Every write is validated and enqueues
 * its outbox row in the same transaction (ADR-0023).
 */
interface WorkoutTemplateRepository {
    /** Creation order (a UUID v7 id). */
    fun observeTemplates(): Flow<List<WorkoutTemplate>>

    fun observeTemplate(id: WorkoutTemplateId): Flow<WorkoutTemplate?>

    suspend fun saveTemplate(template: WorkoutTemplate): DomainResult<Unit, ValidationError>

    /** Tombstones [id] and enqueues a DELETE, held for `DELETE_UNDO_WINDOW`. */
    suspend fun deleteTemplate(id: WorkoutTemplateId)

    /** Clears the tombstone and enqueues an UPSERT, coalescing the held DELETE away, then requests a sync. */
    suspend fun undoTemplateDeletion(id: WorkoutTemplateId)

    /** The undo snackbar was dismissed: releases the held DELETE, then requests a sync. */
    suspend fun confirmTemplateDeletion(id: WorkoutTemplateId)
}
