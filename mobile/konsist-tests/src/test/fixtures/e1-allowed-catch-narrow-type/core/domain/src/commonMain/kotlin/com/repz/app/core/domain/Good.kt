package com.repz.app.core.domain

class Good {
    fun risky() {
        try {
            doWork()
        } catch (e: NumberFormatException) {
            log(e)
        }
    }

    private fun doWork() {}

    private fun log(e: NumberFormatException) {}
}
