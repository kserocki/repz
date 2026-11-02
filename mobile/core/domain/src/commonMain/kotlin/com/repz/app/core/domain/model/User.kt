package com.repz.app.core.domain.model

data class User(
    val id: UserId,
    val email: String,
    val displayName: String,
    val unitSystem: UnitSystem,
)
