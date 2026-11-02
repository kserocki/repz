package com.repz.app.core.domain

class Bad {
    fun risky() {
        try {
            doWork()
        } catch (e: RuntimeException) {
            log(e)
        }
    }

    private fun doWork() {}

    private fun log(e: RuntimeException) {}
}
