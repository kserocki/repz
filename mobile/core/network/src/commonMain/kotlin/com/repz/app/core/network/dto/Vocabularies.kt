package com.repz.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the server's closed `SYNC_TYPES` vocabulary. */
@Serializable
enum class SyncTypeDto {
    @SerialName("workout")
    WORKOUT,

    @SerialName("template")
    TEMPLATE,

    @SerialName("exercise")
    EXERCISE,
}

/** Mirrors the server's closed `SET_TYPES` vocabulary. */
@Serializable
enum class SetTypeDto {
    @SerialName("warmup")
    WARMUP,

    @SerialName("normal")
    NORMAL,

    @SerialName("drop")
    DROP,

    @SerialName("failure")
    FAILURE,
}

/** Mirrors the server's closed `MUSCLES` vocabulary and order. */
@Serializable
enum class MuscleDto {
    @SerialName("chest")
    CHEST,

    @SerialName("back")
    BACK,

    @SerialName("shoulders")
    SHOULDERS,

    @SerialName("biceps")
    BICEPS,

    @SerialName("triceps")
    TRICEPS,

    @SerialName("forearms")
    FOREARMS,

    @SerialName("abs")
    ABS,

    @SerialName("glutes")
    GLUTES,

    @SerialName("quadriceps")
    QUADRICEPS,

    @SerialName("hamstrings")
    HAMSTRINGS,

    @SerialName("calves")
    CALVES,

    @SerialName("full_body")
    FULL_BODY,
}

/** Mirrors the server's closed `EQUIPMENT` vocabulary and order. */
@Serializable
enum class EquipmentDto {
    @SerialName("barbell")
    BARBELL,

    @SerialName("dumbbell")
    DUMBBELL,

    @SerialName("kettlebell")
    KETTLEBELL,

    @SerialName("machine")
    MACHINE,

    @SerialName("cable")
    CABLE,

    @SerialName("bodyweight")
    BODYWEIGHT,

    @SerialName("band")
    BAND,

    @SerialName("other")
    OTHER,
}

@Serializable
enum class UnitSystemDto {
    @SerialName("kg")
    KG,

    @SerialName("lb")
    LB,
}
