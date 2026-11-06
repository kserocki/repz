package com.repz.app.core.sync.template

import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.template.TemplateDao
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.sync.AcceptedChangeDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.SyncChangeDto
import com.repz.app.core.sync.handler.SyncHandler
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TemplateSyncHandler(
    private val templateDao: TemplateDao,
) : SyncHandler {
    override val entityType = SyncEntityType.TEMPLATE
    override val wireType = SyncTypeDto.TEMPLATE

    override suspend fun buildUpsert(id: Uuid): PushChangeDto? {
        val aggregate = templateDao.findTemplateIncludingDeleted(id) ?: return null
        return PushChangeDto.UpsertTemplate(id, aggregate.toUpsertPayload())
    }

    override suspend fun applyAccepted(
        accepted: AcceptedChangeDto,
        now: Instant,
    ): Set<Uuid> {
        if (accepted.version == null) return emptySet()
        if (accepted.deleted) {
            templateDao.setDeleted(accepted.id, isDeleted = true, updatedAt = now)
            templateDao.deleteChildren(accepted.id)
        }
        templateDao.setServerVersion(accepted.id, accepted.version)
        return emptySet()
    }

    override suspend fun applyPulled(change: SyncChangeDto): Set<Uuid> {
        when (change) {
            is SyncChangeDto.Template -> {
                val (entity, exercises) = change.toEntities()
                templateDao.saveSyncedTemplate(entity, exercises)
            }

            is SyncChangeDto.Tombstone -> {
                templateDao.setDeleted(change.id, isDeleted = true, updatedAt = change.updatedAt)
                templateDao.deleteChildren(change.id)
                templateDao.setServerVersion(change.id, change.version)
            }

            else -> {
                error("TemplateSyncHandler can't apply $change")
            }
        }
        return emptySet()
    }
}
