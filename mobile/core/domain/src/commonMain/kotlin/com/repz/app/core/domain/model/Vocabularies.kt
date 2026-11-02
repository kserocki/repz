package com.repz.app.core.domain.model

/** Mirrors the server's closed `MUSCLES` vocabulary and order (ADR-0019); also drives display order for [Exercise.secondaryMuscles]. */
enum class Muscle {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    GLUTES,
    QUADRICEPS,
    HAMSTRINGS,
    CALVES,
    FULL_BODY,
}

/** Mirrors the server's closed `EQUIPMENT` vocabulary and order (ADR-0019). */
enum class Equipment {
    BARBELL,
    DUMBBELL,
    KETTLEBELL,
    MACHINE,
    CABLE,
    BODYWEIGHT,
    BAND,
    OTHER,
}

/** Mirrors the server's closed `SET_TYPES` vocabulary and order (ADR-0019). */
enum class SetType {
    WARMUP,
    NORMAL,
    DROP,
    FAILURE,
}

enum class UnitSystem {
    KG,
    LB,
}
