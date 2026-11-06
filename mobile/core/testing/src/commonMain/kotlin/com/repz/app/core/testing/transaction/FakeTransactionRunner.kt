package com.repz.app.core.testing.transaction

import com.repz.app.core.domain.transaction.TransactionRunner

/** A hand-written fake (N7): runs [block] directly, with no real transaction; [callCount] counts how many times it ran one. */
class FakeTransactionRunner : TransactionRunner {
    var callCount: Int = 0
        private set

    override suspend fun <T> inWriteTransaction(block: suspend () -> T): T {
        callCount++
        return block()
    }
}
