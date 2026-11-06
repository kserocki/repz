package com.repz.app.core.domain.transaction

/** Runs [block] in one local write transaction; repository calls inside it join that transaction. */
interface TransactionRunner {
    suspend fun <T> inWriteTransaction(block: suspend () -> T): T
}
