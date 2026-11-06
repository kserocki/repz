package com.repz.app.core.sync.schedule

import android.content.Context
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A [WorkerParameters] built from hand-written fakes (no mocking library), so a host-JVM test can
 * construct a real [androidx.work.ListenableWorker] without WorkManager itself running. None of
 * these fakes are ever asked to do real work: [SyncWorker] only reads [WorkerParameters.inputData]
 * and the other getters.
 */
private val FAKE_WORKER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

internal fun fakeWorkerParameters(): WorkerParameters =
    WorkerParameters(
        FAKE_WORKER_ID,
        Data.EMPTY,
        emptyList(),
        WorkerParameters.RuntimeExtras(),
        0,
        0,
        ImmediateExecutor,
        EmptyCoroutineContext,
        FakeTaskExecutor,
        NoOpWorkerFactory,
        FakeProgressUpdater,
        FakeForegroundUpdater,
    )

private object ImmediateExecutor : Executor {
    override fun execute(command: Runnable) = command.run()
}

private object FakeSerialExecutor : SerialExecutor, Executor by ImmediateExecutor {
    override fun hasPendingTasks() = false
}

private object FakeTaskExecutor : TaskExecutor {
    override fun getMainThreadExecutor(): Executor = ImmediateExecutor

    override fun getSerialTaskExecutor(): SerialExecutor = FakeSerialExecutor

    override fun getCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
}

private object NoOpWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = null
}

private fun <T> immediateFuture(value: T?): ListenableFuture<T> =
    object : ListenableFuture<T> {
        override fun addListener(
            listener: Runnable,
            executor: Executor,
        ) = executor.execute(listener)

        override fun cancel(mayInterruptIfRunning: Boolean) = false

        override fun isCancelled() = false

        override fun isDone() = true

        override fun get(): T? = value

        override fun get(
            timeout: Long,
            unit: TimeUnit,
        ): T? = value
    }

private object FakeProgressUpdater : ProgressUpdater {
    override fun updateProgress(
        context: Context,
        id: UUID,
        data: Data,
    ): ListenableFuture<Void> = immediateFuture(null)
}

private object FakeForegroundUpdater : ForegroundUpdater {
    override fun setForegroundAsync(
        context: Context,
        id: UUID,
        foregroundInfo: ForegroundInfo,
    ): ListenableFuture<Void> = immediateFuture(null)
}
