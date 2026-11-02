package com.repz.app.core.domain

import kotlin.test.Test

class GoodTest {
    @Test
    fun given_riskyCall_when_runCatching_thenFailureCaptured() {
        val result = runCatching { 1 / 0 }

        check(result.isFailure)
    }
}
