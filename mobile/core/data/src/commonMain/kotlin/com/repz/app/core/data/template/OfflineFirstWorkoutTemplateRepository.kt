package com.repz.app.core.data.template

import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.sync.OutboxDao
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.template.TemplateDao
import com.repz.app.core.domain.model.WorkoutTemplate
import com.repz.app.core.domain.model.WorkoutTemplateId
import com.repz.app.core.domain.result.DomainResult
import com.repz.app.core.domain.result.ValidationError
import com.repz.app.core.domain.sync.DELETE_UNDO_WINDOW
import com.repz.app.core.domain.sync.SyncScheduler
import com.repz.app.core.domain.template.WorkoutTemplateRepository
import com.repz.app.core.domain.validation.validateWorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlin.time.Clock

internal const val TEMPLATE_LOG_TAG = "WorkoutTemplate"

/** Rule 6 and ADR-0023: every save validates first, then writes its entity plus an outbox row in one transaction (R6, R7). */
@Single
class OfflineFirstWorkoutTemplateRepository(
    private val database: RepzDatabase,
    private val templateDao: TemplateDao,
    private val outboxDao: OutboxDao,
    private val syncScheduler: SyncScheduler,
    private val clock: Clock,
    private val logger: Logger = Logger.withTag(TEMPLATE_LOG_TAG),
) : WorkoutTemplateRepository {
    override fun observeTemplates(): Flow<List<WorkoutTemplate>> = templateDao.observeTemplates().map { it.toDomainList(logger) }

    override fun observeTemplate(id: WorkoutTemplateId): Flow<WorkoutTemplate?> =
        templateDao.observeTemplate(id.value).map { it?.toDomainOrNull(logger) }

    override suspend fun saveTemplate(template: WorkoutTemplate): DomainResult<Unit, ValidationError> =
        when (val validation = validateWorkoutTemplate(template)) {
            is DomainResult.Success -> {
                val now = clock.now()
                database.withWriteTransaction {
                    templateDao.saveLocalTemplate(template.toEntity(now), template.toExerciseEntities())
                    outboxDao.enqueue(SyncEntityType.TEMPLATE, template.id.value, OutboxOperation.UPSERT, now)
                }
                syncScheduler.requestSync()
                DomainResult.Success(Unit)
            }

            is DomainResult.Failure -> {
                validation
            }
        }

    override suspend fun deleteTemplate(id: WorkoutTemplateId) {
        val now = clock.now()
        database.withWriteTransaction {
            templateDao.setDeleted(id.value, isDeleted = true, updatedAt = now)
            outboxDao.enqueue(SyncEntityType.TEMPLATE, id.value, OutboxOperation.DELETE, now, heldUntil = now + DELETE_UNDO_WINDOW)
        }
    }

    override suspend fun undoTemplateDeletion(id: WorkoutTemplateId) {
        val now = clock.now()
        database.withWriteTransaction {
            templateDao.setDeleted(id.value, isDeleted = false, updatedAt = now)
            outboxDao.enqueue(SyncEntityType.TEMPLATE, id.value, OutboxOperation.UPSERT, now)
        }
        syncScheduler.requestSync()
    }

    override suspend fun confirmTemplateDeletion(id: WorkoutTemplateId) {
        outboxDao.releaseHold(SyncEntityType.TEMPLATE, id.value)
        syncScheduler.requestSync()
    }
}
