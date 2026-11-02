package com.repz.app.core.domain

class Bad {
    fun risky(result: Result<Int>): Result<Int> = result.mapCatching { it / 0 }
}
