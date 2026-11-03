package com.repz.app.core.database.sync

/** Local-only; Room stores these by name. The wire `type` string is `core:sync`'s concern. */
enum class SyncEntityType {
    WORKOUT,
    TEMPLATE,
    EXERCISE,
}

enum class OutboxOperation {
    UPSERT,
    DELETE,
}
