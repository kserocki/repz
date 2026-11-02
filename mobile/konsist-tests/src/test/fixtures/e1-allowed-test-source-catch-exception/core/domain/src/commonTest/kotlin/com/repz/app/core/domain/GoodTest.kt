package com.repz.app.core.domain

import kotlin.test.Test
import kotlin.test.fail

class GoodTest {
    @Test
    fun given_riskyCall_when_itThrows_thenAssertedDirectly() {
        try {
            doWork()
            fail("expected an exception")
        } catch (e: Exception) {
            check(e.message == "boom")
        }
    }

    private fun doWork() {
        throw IllegalStateException("boom")
    }
}
