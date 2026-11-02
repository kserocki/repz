package com.repz.app.core.domain

class Bad {
    fun risky() {
        try {
            doWork()
        } catch (e: Exception) {
            throw IllegalArgumentException("wrapped", e)
        }
    }

    private fun doWork() {}
}
