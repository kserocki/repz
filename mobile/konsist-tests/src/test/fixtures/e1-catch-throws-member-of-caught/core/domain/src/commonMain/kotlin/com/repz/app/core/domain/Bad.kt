package com.repz.app.core.domain

class Bad {
    fun risky() {
        try {
            doWork()
        } catch (e: Exception) {
            throw e.toDomainError()
        }
    }

    private fun doWork() {}
}

private fun Exception.toDomainError(): Exception = IllegalStateException(message, this)
