package com.repz.app.core.domain

class Good {
    fun risky() {
        try {
            doWork()
        } catch (e: Throwable) {
            log(e)
            throw e
        }
    }

    private fun doWork() {}

    private fun log(e: Throwable) {}
}
