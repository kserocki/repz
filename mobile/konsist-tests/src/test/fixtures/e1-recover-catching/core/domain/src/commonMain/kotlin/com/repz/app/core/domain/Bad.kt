package com.repz.app.core.domain

class Bad {
    fun risky(result: Result<Int>): Result<Int> = result.recoverCatching { riskyFallback() }

    private fun riskyFallback(): Int = 0
}
