package com.repz.app.core.database

import kotlin.uuid.Uuid

/** A deterministic, literal id for tests: [suffix] zero-padded into an otherwise-fixed UUID text. */
internal fun testUuid(suffix: String): Uuid = Uuid.parse("00000000-0000-7000-8000-" + suffix.padStart(12, '0'))
