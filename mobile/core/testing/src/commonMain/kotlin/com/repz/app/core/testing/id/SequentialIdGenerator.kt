package com.repz.app.core.testing.id

import com.repz.app.core.common.id.IdGenerator
import kotlin.uuid.Uuid

/**
 * A deterministic [IdGenerator] for tests (I1 forbids random id generation outside the real
 * generator, tests included): each call zero-pads an incrementing counter into an otherwise-fixed
 * UUID, the same shape the `testUuid` test helpers in `core:domain` build by hand.
 */
class SequentialIdGenerator(
    private var next: Int = 1,
) : IdGenerator {
    override fun newId(): Uuid = Uuid.parse("00000000-0000-0000-0000-" + (next++).toString().padStart(length = 12, padChar = '0'))
}
