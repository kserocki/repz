package com.repz.app.core.data

import kotlin.uuid.Uuid

/** A deterministic, literal id for tests (Konsist I1 forbids `Uuid.random` even in tests): [suffix] zero-padded into a fixed UUID text. */
internal fun testUuid(suffix: String): Uuid = Uuid.parse("00000000-0000-7000-8000-" + suffix.padStart(12, '0'))
