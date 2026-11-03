package com.repz.app.core.designsystem.catalog

import androidx.compose.runtime.mutableStateListOf
import com.repz.app.core.designsystem.haptics.RepzHapticEvent
import com.repz.app.core.designsystem.haptics.RepzHapticPerformer

private const val HAPTIC_LOG_SIZE = 10

/**
 * Wraps the real platform performer so the `haptics` catalog page can show what fired, in order,
 * without a physical device: every invocation is recorded as `#n EventName`, newest first, capped
 * to the last [HAPTIC_LOG_SIZE]. [recordBlocked] lets the page log a tap that `RepzHaptics` itself
 * swallowed because haptics are disabled (it never reaches [delegate] in that case).
 */
internal class CatalogHapticLog(
    private val delegate: RepzHapticPerformer,
) : RepzHapticPerformer {
    private val mutableEntries = mutableStateListOf<String>()
    val entries: List<String> get() = mutableEntries
    private var count = 0

    override fun perform(event: RepzHapticEvent) {
        delegate.perform(event)
        record(event.name)
    }

    fun recordBlocked(event: RepzHapticEvent) {
        record("${event.name} (blocked: disabled)")
    }

    private fun record(label: String) {
        count += 1
        mutableEntries.add(0, "#$count $label")
        while (mutableEntries.size > HAPTIC_LOG_SIZE) {
            mutableEntries.removeAt(mutableEntries.lastIndex)
        }
    }
}
