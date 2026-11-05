package com.repz.app.core.data.user

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
