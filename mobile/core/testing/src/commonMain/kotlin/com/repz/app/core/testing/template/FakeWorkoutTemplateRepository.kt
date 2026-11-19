package com.repz.app.core.testing.template

import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.template.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A hand-written fake (N7): an in-memory store keyed by [WorkoutTemplate.id], tombstones included,
 * the same shape as [com.repz.app.core.testing.workout.FakeWorkoutRepository].
 * [saveTemplateResult] is returned as-is, without writing, when it is a configured
 * [DomainResult.Failure]; every write/delete/undo/confirm call is recorded for assertions.
 */
class FakeWorkoutTemplateRepository(
    initial: List<WorkoutTemplate> = emptyList(),
    var saveTemplateResult: DomainResult<Unit, ValidationError>? = null,
) : WorkoutTemplateRepository {
    private data class Entry(
        val template: WorkoutTemplate,
        val isDeleted: Boolean = false,
    )

    private val entries = MutableStateFlow(initial.associate { it.id to Entry(it) })

    val saveTemplateCalls = mutableListOf<WorkoutTemplate>()
    val deleteTemplateCalls = mutableListOf<WorkoutTemplateId>()
    val undoTemplateDeletionCalls = mutableListOf<WorkoutTemplateId>()
    val confirmTemplateDeletionCalls = mutableListOf<WorkoutTemplateId>()

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        entries.map { current -> current.values.filterNot { it.isDeleted }.map { it.template } }

    override fun observeTemplate(id: WorkoutTemplateId): Flow<WorkoutTemplate?> =
        entries.map { current -> current[id]?.takeUnless { it.isDeleted }?.template }

    override suspend fun saveTemplate(template: WorkoutTemplate): DomainResult<Unit, ValidationError> {
        saveTemplateCalls += template
        val configured = saveTemplateResult
        if (configured is DomainResult.Failure) return configured
        entries.update { it + (template.id to Entry(template)) }
        return DomainResult.Success(Unit)
    }

    override suspend fun deleteTemplate(id: WorkoutTemplateId) {
        deleteTemplateCalls += id
        entries.update { current -> current[id]?.let { current + (id to it.copy(isDeleted = true)) } ?: current }
    }

    override suspend fun undoTemplateDeletion(id: WorkoutTemplateId) {
        undoTemplateDeletionCalls += id
        entries.update { current -> current[id]?.let { current + (id to it.copy(isDeleted = false)) } ?: current }
    }

    override suspend fun confirmTemplateDeletion(id: WorkoutTemplateId) {
        confirmTemplateDeletionCalls += id
    }
}
