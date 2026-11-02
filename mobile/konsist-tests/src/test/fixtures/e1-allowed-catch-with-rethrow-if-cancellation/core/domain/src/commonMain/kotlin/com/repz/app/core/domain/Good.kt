package com.repz.app.core.domain

import com.repz.app.core.common.coroutines.rethrowIfCancellation

class Good {
    fun risky() {
        try {
            doWork()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            log(e)
        }
    }

    private fun doWork() {}

    private fun log(e: Exception) {}
}
