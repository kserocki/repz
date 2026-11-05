package com.repz.app.core.data.user

import com.repz.app.core.database.account.CurrentUserEntity
import com.repz.app.core.domain.model.UnitSystem
import com.repz.app.core.domain.model.User
import com.repz.app.core.domain.model.UserId
import com.repz.app.core.network.dto.UnitSystemDto
import com.repz.app.core.network.dto.user.UserDto

internal fun UserDto.toDomain(): User =
    User(
        id = UserId(id),
        email = email,
        displayName = displayName,
        unitSystem = unitSystem.toDomain(),
    )

internal fun UnitSystemDto.toDomain(): UnitSystem =
    when (this) {
        UnitSystemDto.KG -> UnitSystem.KG
        UnitSystemDto.LB -> UnitSystem.LB
    }

/** [CurrentUserEntity.unitSystem] holds the same lowercase snake_case vocabulary text as the wire (ADR-0023). */
internal fun UserDto.toCurrentUserEntity(): CurrentUserEntity =
    CurrentUserEntity(
        id = id,
        email = email,
        displayName = displayName,
        unitSystem = unitSystem.toEntityText(),
    )

internal fun CurrentUserEntity.toDomain(): User =
    User(
        id = UserId(id),
        email = email,
        displayName = displayName,
        unitSystem = unitSystem.toUnitSystem(),
    )

private fun UnitSystemDto.toEntityText(): String =
    when (this) {
        UnitSystemDto.KG -> "kg"
        UnitSystemDto.LB -> "lb"
    }

private fun String.toUnitSystem(): UnitSystem =
    when (this) {
        "kg" -> UnitSystem.KG
        "lb" -> UnitSystem.LB
        else -> error("Unknown current_user.unit_system value: $this")
    }
