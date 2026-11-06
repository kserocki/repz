package com.repz.app.core.sync.engine

import com.repz.app.core.database.RepzDatabase
import com.repz.app.core.database.exercise.ExerciseEntity
import com.repz.app.core.database.sync.OutboxOperation
import com.repz.app.core.database.sync.SyncEntityType
import com.repz.app.core.database.sync.SyncRejectionEntity
import com.repz.app.core.database.workout.WorkoutEntity
import com.repz.app.core.database.workout.WorkoutExerciseEntity
import com.repz.app.core.database.workout.WorkoutSetEntity
import com.repz.app.core.domain.sync.SyncFailureReason
import com.repz.app.core.network.dto.EquipmentDto
import com.repz.app.core.network.dto.MuscleDto
import com.repz.app.core.network.dto.SyncTypeDto
import com.repz.app.core.network.dto.exercise.CustomExercisePayloadDto
import com.repz.app.core.network.dto.problem.ItemProblemDto
import com.repz.app.core.network.dto.sync.AcceptedChangeDto
import com.repz.app.core.network.dto.sync.PushChangeDto
import com.repz.app.core.network.dto.sync.PushResponseDto
import com.repz.app.core.network.dto.sync.RejectedChangeDto
import com.repz.app.core.network.result.NetworkError
import com.repz.app.core.network.result.NetworkResult
import com.repz.app.core.sync.fakes.FakeSyncApi
import com.repz.app.core.sync.inMemoryRepzDatabase
import com.repz.app.core.sync.testSyncEngine
import com.repz.app.core.sync.testUuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

private val FINISHED_AT = Instant.fromEpochMilliseconds(1_000)

private suspend fun RepzDatabase.insertFinishedWorkout(
    id: Uuid,
    exerciseId: Uuid,
    workoutExerciseId: Uuid,
    setId: Uuid,
) {
    workoutDao().saveSyncedWorkout(
        WorkoutEntity(
            id = id,
            templateId = null,
            name = "Push day",
            notes = "",
            startedAt = FINISHED_AT,
            finishedAt = FINISHED_AT,
            updatedAt = FINISHED_AT,
            serverVersion = null,
            isDeleted = false,
        ),
        listOf(WorkoutExerciseEntity(workoutExerciseId, id, exerciseId, position = 0, notes = "")),
        listOf(WorkoutSetEntity(setId, workoutExerciseId, 0, "normal", 40_000, 10, null, FINISHED_AT)),
    )
}

class SyncEnginePushTest {
    @Test
    fun givenAReadyOutboxRowAndAPullPage_whenSyncing_thenPushesBeforePulling() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            harness.database.insertFinishedWorkout(workoutId, testUuid("2"), testUuid("3"), testUuid("4"))
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, FINISHED_AT)

            harness.engine.sync()

            assertEquals(listOf("push", "pull"), syncApi.callLog)
        }

    @Test
    fun givenAnAcceptedUpsert_whenSyncing_thenSetsServerVersionAndClearsTheOutboxRowAndAnyRejectionMark() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            harness.database.insertFinishedWorkout(workoutId, testUuid("2"), testUuid("3"), testUuid("4"))
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, FINISHED_AT)
            harness.database.syncRejectionDao().upsert(
                SyncRejectionEntity(SyncEntityType.WORKOUT, workoutId, "urn:repz:problem:id-conflict", FINISHED_AT),
            )
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(listOf(AcceptedChangeDto(SyncTypeDto.WORKOUT, workoutId, 5, deleted = false)), emptyList()),
                ),
            )

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.Completed, outcome)
            assertEquals(
                5L,
                harness.database
                    .workoutDao()
                    .findWorkoutIncludingDeleted(workoutId)
                    ?.workout
                    ?.serverVersion,
            )
            assertEquals(0, harness.database.outboxDao().countPending())
            assertEquals(
                0,
                harness.database
                    .syncRejectionDao()
                    .observeCount()
                    .first(),
            )
        }

    @Test
    fun givenTheOutboxRowChangesWhileThePushIsInFlight_whenTheResponseArrives_thenTheRowSurvives() =
        runTest {
            val syncApi =
                FakeSyncApi().apply {
                    pushGate = CompletableDeferred()
                    reachedPushGate = CompletableDeferred()
                    // Isolate the one response under test: no auto-accept on a later (final-pass) push.
                    pushHandler = { NetworkResult.Success(PushResponseDto(emptyList(), emptyList())) }
                }
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            harness.database.insertFinishedWorkout(workoutId, testUuid("2"), testUuid("3"), testUuid("4"))
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, FINISHED_AT)
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(listOf(AcceptedChangeDto(SyncTypeDto.WORKOUT, workoutId, 9, deleted = false)), emptyList()),
                ),
            )

            val sync = launch { harness.engine.sync() }
            syncApi.reachedPushGate!!.await()
            // A local edit lands while the request the engine already sent is still in flight.
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, FINISHED_AT)
            syncApi.pushGate!!.complete(Unit)
            sync.join()

            assertEquals(1, harness.database.outboxDao().countPending())
        }

    @Test
    fun givenARejectedUpsert_whenSyncing_thenMarksItAndClearsTheOutboxButKeepsLocalDataUnchanged() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            harness.database.insertFinishedWorkout(workoutId, testUuid("2"), testUuid("3"), testUuid("4"))
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, FINISHED_AT)
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(
                        emptyList(),
                        listOf(
                            RejectedChangeDto(
                                SyncTypeDto.WORKOUT,
                                workoutId,
                                ItemProblemDto("urn:repz:problem:id-conflict", "Id Conflict"),
                            ),
                        ),
                    ),
                ),
            )

            harness.engine.sync()

            assertEquals(0, harness.database.outboxDao().countPending())
            val stored = checkNotNull(harness.database.workoutDao().findWorkoutIncludingDeleted(workoutId))
            assertNull(stored.workout.serverVersion)
            assertFalse(stored.workout.isDeleted)
        }

    @Test
    fun givenAnUpsertAcceptedAsDeleted_whenSyncing_thenTombstonesAndDeletesChildrenAndRefreshesItsExerciseIds() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            val exerciseId = testUuid("2")
            harness.database.insertFinishedWorkout(workoutId, exerciseId, testUuid("3"), testUuid("4"))
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.UPSERT, FINISHED_AT)
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(listOf(AcceptedChangeDto(SyncTypeDto.WORKOUT, workoutId, 3, deleted = true)), emptyList()),
                ),
            )

            harness.engine.sync()

            val stored = checkNotNull(harness.database.workoutDao().findWorkoutIncludingDeleted(workoutId))
            assertTrue(stored.workout.isDeleted)
            assertEquals(3L, stored.workout.serverVersion)
            assertTrue(stored.exercises.isEmpty())
            assertEquals(listOf(setOf(exerciseId)), harness.recordsRefresher.refreshForExercisesCalls)
        }

    @Test
    fun givenAnAcceptedChangeWithNoServerVersion_whenSyncing_thenOnlyClearsTheOutboxRow() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.DELETE, FINISHED_AT)
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(listOf(AcceptedChangeDto(SyncTypeDto.WORKOUT, workoutId, null, deleted = true)), emptyList()),
                ),
            )

            harness.engine.sync()

            assertEquals(0, harness.database.outboxDao().countPending())
            assertTrue(harness.recordsRefresher.refreshForExercisesCalls.isEmpty())
            assertNull(harness.database.workoutDao().findWorkoutIncludingDeleted(workoutId))
        }

    @Test
    fun givenAHeldDelete_whenSyncing_thenItIsNotPushedBeforeItsHoldExpiresButIsAfterTheHoldIsReleased() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            harness.database.outboxDao().enqueue(
                SyncEntityType.WORKOUT,
                workoutId,
                OutboxOperation.DELETE,
                FINISHED_AT,
                heldUntil = FINISHED_AT + 10.minutes,
            )

            harness.engine.sync()
            assertTrue(syncApi.pushRequests.isEmpty())

            harness.database.outboxDao().releaseHold(SyncEntityType.WORKOUT, workoutId)
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(listOf(AcceptedChangeDto(SyncTypeDto.WORKOUT, workoutId, 1, deleted = true)), emptyList()),
                ),
            )
            harness.engine.sync()

            assertEquals(1, syncApi.pushRequests.size)
        }

    @Test
    fun given250ReadyRows_whenSyncing_thenPushesThemInThreeRequestsOfAtMost100() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            repeat(250) { index ->
                harness.database.outboxDao().enqueue(
                    SyncEntityType.WORKOUT,
                    testUuid(index.toString()),
                    OutboxOperation.DELETE,
                    FINISHED_AT,
                )
            }

            harness.engine.sync()

            assertEquals(listOf(100, 100, 50), syncApi.pushRequests.map { it.changes.size })
        }

    @Test
    fun givenA413OnTheWholeBatch_whenSyncing_thenSplitsItInHalfAndRetries() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val ids = (1..4).map { testUuid(it.toString()) }
            ids.forEach { harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, it, OutboxOperation.DELETE, FINISHED_AT) }
            syncApi.enqueuePushResponse(NetworkResult.Failure(NetworkError.Api(413, null)))
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(ids.take(2).map { AcceptedChangeDto(SyncTypeDto.WORKOUT, it, 1, deleted = true) }, emptyList()),
                ),
            )
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(ids.drop(2).map { AcceptedChangeDto(SyncTypeDto.WORKOUT, it, 1, deleted = true) }, emptyList()),
                ),
            )

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.Completed, outcome)
            assertEquals(3, syncApi.pushRequests.size)
            assertEquals(0, harness.database.outboxDao().countPending())
        }

    @Test
    fun givenAnUpsertRowWhoseEntityIsMissing_whenSyncing_thenItIsDropped() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val missingWorkoutId = testUuid("1")
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, missingWorkoutId, OutboxOperation.UPSERT, FINISHED_AT)

            harness.engine.sync()

            assertTrue(syncApi.pushRequests.isEmpty())
            assertEquals(0, harness.database.outboxDao().countPending())
        }

    /**
     * [OutboxPusher] reads an outbox row (its revision included) once per round, then - only if
     * [com.repz.app.core.sync.handler.SyncHandler.buildUpsert] finds nothing to push - drops that
     * same row via `deleteIfUnchanged(type, id, revision)`. A real edit landing in that narrow window
     * isn't reproducible from outside without a dedicated test seam (both steps are plain,
     * un-gated suspend calls within one loop iteration), so this exercises the exact guard call
     * directly: a revision read before a concurrent edit must not be allowed to drop the edited row.
     */
    @Test
    fun givenTheRowWasEditedAfterItWasReadAndBeforeItWasDropped_whenDroppingWithTheStaleRevision_thenTheRowSurvives() =
        runTest {
            val database = inMemoryRepzDatabase()
            val missingWorkoutId = testUuid("1")
            database.outboxDao().enqueue(SyncEntityType.WORKOUT, missingWorkoutId, OutboxOperation.UPSERT, FINISHED_AT)
            val readBeforeTheEdit = database.outboxDao().findReadyToPush(FINISHED_AT, limit = 10).single()
            // A local edit lands after this revision was read, but before the drop below runs.
            database.outboxDao().enqueue(SyncEntityType.WORKOUT, missingWorkoutId, OutboxOperation.UPSERT, FINISHED_AT)

            val deletedRows = database.outboxDao().deleteIfUnchanged(SyncEntityType.WORKOUT, missingWorkoutId, readBeforeTheEdit.revision)

            assertEquals(0, deletedRows)
            assertEquals(1, database.outboxDao().countPending())
        }

    @Test
    fun givenACustomExerciseUpsert_whenSyncing_thenSendsItsVocabularyOnTheWire() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val exerciseId = testUuid("1")
            harness.database.exerciseDao().saveSyncedExercise(
                ExerciseEntity(
                    id = exerciseId,
                    name = "My curl",
                    primaryMuscle = "biceps",
                    secondaryMuscles = listOf("forearms"),
                    equipment = "dumbbell",
                    isCustom = true,
                    imageUrl = null,
                    license = null,
                    author = null,
                    sourceUrl = null,
                    updatedAt = FINISHED_AT,
                    serverVersion = null,
                    isDeleted = false,
                ),
                emptyList(),
            )
            harness.database.outboxDao().enqueue(SyncEntityType.EXERCISE, exerciseId, OutboxOperation.UPSERT, FINISHED_AT)
            syncApi.enqueuePushResponse(
                NetworkResult.Success(
                    PushResponseDto(listOf(AcceptedChangeDto(SyncTypeDto.EXERCISE, exerciseId, 1, deleted = false)), emptyList()),
                ),
            )

            harness.engine.sync()

            val sentChange =
                syncApi.pushRequests
                    .single()
                    .changes
                    .single()
            val sentPayload = (sentChange as PushChangeDto.UpsertCustomExercise).payload
            assertEquals(
                CustomExercisePayloadDto("My curl", MuscleDto.BICEPS, listOf(MuscleDto.FOREARMS), EquipmentDto.DUMBBELL),
                sentPayload,
            )
        }

    @Test
    fun givenAWholeRequest4xxOnPush_whenSyncing_thenFailsBumpsAttemptsOnEveryRowAndNeverPulls() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val ids = (1..2).map { testUuid(it.toString()) }
            ids.forEach { harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, it, OutboxOperation.DELETE, FINISHED_AT) }
            syncApi.enqueuePushResponse(NetworkResult.Failure(NetworkError.Api(422, null)))

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.Failed(SyncFailureReason.Unexpected), outcome)
            assertTrue(syncApi.pullCalls.isEmpty())
            val entries = harness.database.outboxDao().findReadyToPush(FINISHED_AT, limit = 10)
            assertEquals(ids.toSet(), entries.map { it.entityId }.toSet())
            assertTrue(entries.all { it.attempts == 1 })
        }

    @Test
    fun givenOfflineOnPush_whenSyncing_thenReturnsRetryLaterWithoutBumpingAttemptsOrPulling() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val workoutId = testUuid("1")
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, workoutId, OutboxOperation.DELETE, FINISHED_AT)
            syncApi.enqueuePushResponse(NetworkResult.Failure(NetworkError.Offline))

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.RetryLater(null), outcome)
            assertTrue(syncApi.pullCalls.isEmpty())
            val entry =
                harness.database
                    .outboxDao()
                    .findReadyToPush(FINISHED_AT, limit = 10)
                    .single()
            assertEquals(0, entry.attempts)
        }

    @Test
    fun givenASingleChangeStillTooLargeAfterSplitting_whenSyncing_thenBumpsItsAttemptsAndStillPushesTheRest() =
        runTest {
            val syncApi = FakeSyncApi()
            val harness = testSyncEngine(backgroundScope, syncApi = syncApi)
            val acceptedId = testUuid("1")
            val tooLargeId = testUuid("2")
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, acceptedId, OutboxOperation.DELETE, FINISHED_AT)
            harness.database.outboxDao().enqueue(SyncEntityType.WORKOUT, tooLargeId, OutboxOperation.DELETE, FINISHED_AT)
            // tooLargeId is scripted to always come back as 413, however small the batch it ends up
            // in (a real body never shrinks on retry either), so every attempt at it keeps failing.
            syncApi.pushHandler = { request ->
                if (request.changes.any { it.id == tooLargeId }) {
                    NetworkResult.Failure(NetworkError.Api(413, null))
                } else {
                    NetworkResult.Success(
                        PushResponseDto(
                            request.changes.map { AcceptedChangeDto(SyncTypeDto.WORKOUT, it.id, 1, deleted = true) },
                            emptyList(),
                        ),
                    )
                }
            }

            val outcome = harness.engine.sync()

            assertEquals(SyncOutcome.Completed, outcome)
            val remaining = harness.database.outboxDao().findReadyToPush(FINISHED_AT, limit = 10)
            assertEquals(listOf(tooLargeId), remaining.map { it.entityId })
            assertTrue(remaining.single().attempts >= 1)
        }
}
