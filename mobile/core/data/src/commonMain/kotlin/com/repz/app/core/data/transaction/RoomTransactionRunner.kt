package com.repz.app.core.data.transaction

import androidx.room3.withWriteTransaction
import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.domain.transaction.TransactionRunner
import org.koin.core.annotation.Single

/** Wraps [RepzDatabase]'s Room 3 write transaction, so a domain use case can run several repository calls atomically. */
@Single
class RoomTransactionRunner(
    private val database: RepzDatabase,
) : TransactionRunner {
    override suspend fun <T> inWriteTransaction(block: suspend () -> T): T = database.withWriteTransaction { block() }
}
