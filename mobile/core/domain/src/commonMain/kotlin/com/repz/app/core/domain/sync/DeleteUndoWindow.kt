package com.repz.app.core.domain.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How long a delete stays held in the outbox while its undo snackbar shows (DESIGN_SYSTEM: 6 s). */
val DELETE_UNDO_WINDOW: Duration = 6.seconds
