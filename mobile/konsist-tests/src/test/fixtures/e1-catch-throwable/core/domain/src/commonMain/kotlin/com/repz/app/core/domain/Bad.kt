package com.repz.app.core.domain

class Bad {
    fun risky() {
        try {
            doWork()
        } catch (e: Throwable) {
            log(e)
        }
    }

    private fun doWork() {}

    private fun log(e: Throwable) {}
}
