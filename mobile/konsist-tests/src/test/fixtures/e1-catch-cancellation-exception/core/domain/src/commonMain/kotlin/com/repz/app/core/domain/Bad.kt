package com.repz.app.core.domain

class Bad {
    fun risky() {
        try {
            doWork()
        } catch (e: CancellationException) {
            log(e)
        }
    }

    private fun doWork() {}

    private fun log(e: CancellationException) {}
}
