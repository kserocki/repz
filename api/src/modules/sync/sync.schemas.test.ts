import { describe, expect, it } from "vitest";
import {
  CustomExercisePayloadSchema,
  NAME_MAX,
  PUSH_MAX_CHANGES,
  PushRequestSchema,
  REPS_MAX,
  SETS_PER_EXERCISE_MAX,
  TARGET_SETS_MAX,
  TemplatePayloadSchema,
  WorkoutPayloadSchema,
} from "./sync.schemas.ts";

const TEMPLATE_ID = "b6e6b6a0-4444-4a22-8a33-000000000001";
const EXERCISE_ID = "b6e6b6a0-3333-4a22-8a33-000000000001";
const WORKOUT_EXERCISE_ID = "b6e6b6a0-2222-4a22-8a33-000000000002";
const SET_ID = "b6e6b6a0-2222-4a22-8a33-000000000003";

function baseWorkout(overrides: Record<string, unknown> = {}) {
  return {
    templateId: null,
    name: "Push day",
    notes: "",
    startedAt: "2026-11-01T10:00:00.000Z",
    finishedAt: "2026-11-01T10:45:00.000Z",
    exercises: [],
    ...overrides,
  };
}

function baseSet(overrides: Record<string, unknown> = {}) {
  return {
    id: SET_ID,
    type: "normal",
    weightGrams: 60_000,
    reps: 8,
    rpe: 8,
    completedAt: "2026-11-01T10:05:00.000Z",
    ...overrides,
  };
}

describe("WorkoutPayloadSchema", () => {
  it.each([6, 6.5, 7, 9.5, 10])("given rpe %s, when parsed, then it is accepted", (rpe) => {
    const workout = baseWorkout({
      exercises: [
        { id: WORKOUT_EXERCISE_ID, exerciseId: EXERCISE_ID, notes: "", sets: [baseSet({ rpe })] },
      ],
    });
    expect(WorkoutPayloadSchema.safeParse(workout).success).toBe(true);
  });

  it.each([5.5, 6.25, 10.5])("given rpe %s, when parsed, then it is rejected", (rpe) => {
    const workout = baseWorkout({
      exercises: [
        { id: WORKOUT_EXERCISE_ID, exerciseId: EXERCISE_ID, notes: "", sets: [baseSet({ rpe })] },
      ],
    });
    expect(WorkoutPayloadSchema.safeParse(workout).success).toBe(false);
  });

  it("given a null rpe, when parsed, then it is accepted", () => {
    const workout = baseWorkout({
      exercises: [
        {
          id: WORKOUT_EXERCISE_ID,
          exerciseId: EXERCISE_ID,
          notes: "",
          sets: [baseSet({ rpe: null })],
        },
      ],
    });
    expect(WorkoutPayloadSchema.safeParse(workout).success).toBe(true);
  });

  it("given finishedAt before startedAt, when parsed, then it is rejected on the finishedAt path", () => {
    const result = WorkoutPayloadSchema.safeParse(
      baseWorkout({
        startedAt: "2026-11-01T10:45:00.000Z",
        finishedAt: "2026-11-01T10:00:00.000Z",
      }),
    );
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path).toEqual(["finishedAt"]);
    }
  });

  it("given finishedAt equal to startedAt, when parsed, then it is accepted", () => {
    expect(
      WorkoutPayloadSchema.safeParse(
        baseWorkout({ startedAt: baseWorkout().startedAt, finishedAt: baseWorkout().startedAt }),
      ).success,
    ).toBe(true);
  });

  it("given a non-UTC offset timestamp, when parsed, then it is accepted", () => {
    expect(
      WorkoutPayloadSchema.safeParse(baseWorkout({ startedAt: "2026-11-01T12:00:00.000+02:00" }))
        .success,
    ).toBe(true);
  });

  it.each([-1, REPS_MAX + 1])("given reps %i, when parsed, then it is rejected", (reps) => {
    const workout = baseWorkout({
      exercises: [
        { id: WORKOUT_EXERCISE_ID, exerciseId: EXERCISE_ID, notes: "", sets: [baseSet({ reps })] },
      ],
    });
    expect(WorkoutPayloadSchema.safeParse(workout).success).toBe(false);
  });

  it(`given ${SETS_PER_EXERCISE_MAX + 1} sets, when parsed, then it is rejected`, () => {
    const sets = Array.from({ length: SETS_PER_EXERCISE_MAX + 1 }, (_, index) =>
      baseSet({ id: `b6e6b6a0-2222-4a22-8a33-${String(index).padStart(12, "0")}` }),
    );
    const workout = baseWorkout({
      exercises: [{ id: WORKOUT_EXERCISE_ID, exerciseId: EXERCISE_ID, notes: "", sets }],
    });
    expect(WorkoutPayloadSchema.safeParse(workout).success).toBe(false);
  });

  it("given no exercises, when parsed, then it is accepted (API accepts a workout with no completed sets)", () => {
    expect(WorkoutPayloadSchema.safeParse(baseWorkout()).success).toBe(true);
  });

  it("given an unknown top-level key, when parsed, then it is rejected (strict object)", () => {
    expect(WorkoutPayloadSchema.safeParse({ ...baseWorkout(), extra: "nope" }).success).toBe(false);
  });

  it("given a name over the max length, when parsed, then it is rejected", () => {
    expect(
      WorkoutPayloadSchema.safeParse(baseWorkout({ name: "a".repeat(NAME_MAX + 1) })).success,
    ).toBe(false);
  });

  it("given a name with surrounding whitespace, when parsed, then it is trimmed", () => {
    const result = WorkoutPayloadSchema.safeParse(baseWorkout({ name: "  Push day  " }));
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.name).toBe("Push day");
    }
  });

  it("given an empty name, when parsed, then it is rejected", () => {
    expect(WorkoutPayloadSchema.safeParse(baseWorkout({ name: "   " })).success).toBe(false);
  });

  it("given two exercises with the same id, when parsed, then it is rejected with a path at the duplicate", () => {
    const duplicateId = WORKOUT_EXERCISE_ID;
    const result = WorkoutPayloadSchema.safeParse(
      baseWorkout({
        exercises: [
          { id: duplicateId, exerciseId: EXERCISE_ID, notes: "", sets: [] },
          { id: duplicateId, exerciseId: EXERCISE_ID, notes: "", sets: [] },
        ],
      }),
    );
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues).toContainEqual(
        expect.objectContaining({ path: ["exercises", 1, "id"] }),
      );
    }
  });

  it("given two sets sharing an id within the same exercise, when parsed, then it is rejected with a path at the duplicate", () => {
    const duplicateSetId = SET_ID;
    const result = WorkoutPayloadSchema.safeParse(
      baseWorkout({
        exercises: [
          {
            id: WORKOUT_EXERCISE_ID,
            exerciseId: EXERCISE_ID,
            notes: "",
            sets: [baseSet({ id: duplicateSetId }), baseSet({ id: duplicateSetId })],
          },
        ],
      }),
    );
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues).toContainEqual(
        expect.objectContaining({ path: ["exercises", 0, "sets", 1, "id"] }),
      );
    }
  });

  it("given two sets sharing an id across different exercises, when parsed, then it is rejected with a path at the duplicate", () => {
    const duplicateSetId = SET_ID;
    const result = WorkoutPayloadSchema.safeParse(
      baseWorkout({
        exercises: [
          {
            id: WORKOUT_EXERCISE_ID,
            exerciseId: EXERCISE_ID,
            notes: "",
            sets: [baseSet({ id: duplicateSetId })],
          },
          {
            id: "b6e6b6a0-2222-4a22-8a33-000000000099",
            exerciseId: EXERCISE_ID,
            notes: "",
            sets: [baseSet({ id: duplicateSetId })],
          },
        ],
      }),
    );
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues).toContainEqual(
        expect.objectContaining({ path: ["exercises", 1, "sets", 0, "id"] }),
      );
    }
  });

  it("given distinct exercise and set ids throughout, when parsed, then it is accepted", () => {
    const result = WorkoutPayloadSchema.safeParse(
      baseWorkout({
        exercises: [
          {
            id: WORKOUT_EXERCISE_ID,
            exerciseId: EXERCISE_ID,
            notes: "",
            sets: [
              baseSet({ id: SET_ID }),
              baseSet({ id: "b6e6b6a0-2222-4a22-8a33-000000000098" }),
            ],
          },
          {
            id: "b6e6b6a0-2222-4a22-8a33-000000000099",
            exerciseId: EXERCISE_ID,
            notes: "",
            sets: [baseSet({ id: "b6e6b6a0-2222-4a22-8a33-000000000097" })],
          },
        ],
      }),
    );
    expect(result.success).toBe(true);
  });
});

describe("TemplatePayloadSchema", () => {
  function baseTemplate(overrides: Record<string, unknown> = {}) {
    return { name: "Push day", notes: "", exercises: [], ...overrides };
  }

  it.each([0, TARGET_SETS_MAX])(
    "given targetSets %i, when parsed, then it is accepted if >= 1",
    (targetSets) => {
      const expected = targetSets >= 1;
      const result = TemplatePayloadSchema.safeParse(
        baseTemplate({
          exercises: [{ exerciseId: EXERCISE_ID, targetSets, targetReps: null, restSeconds: 60 }],
        }),
      );
      expect(result.success).toBe(expected);
    },
  );

  it("given targetReps with min > max, when parsed, then it is rejected", () => {
    const result = TemplatePayloadSchema.safeParse(
      baseTemplate({
        exercises: [
          {
            exerciseId: EXERCISE_ID,
            targetSets: 3,
            targetReps: { min: 12, max: 8 },
            restSeconds: 60,
          },
        ],
      }),
    );
    expect(result.success).toBe(false);
  });

  it("given a null targetReps, when parsed, then it is accepted", () => {
    const result = TemplatePayloadSchema.safeParse(
      baseTemplate({
        exercises: [{ exerciseId: EXERCISE_ID, targetSets: 3, targetReps: null, restSeconds: 60 }],
      }),
    );
    expect(result.success).toBe(true);
  });

  it("given the same exerciseId referenced twice, when parsed, then it is accepted (a template exercise has no id of its own, position is the key)", () => {
    const result = TemplatePayloadSchema.safeParse(
      baseTemplate({
        exercises: [
          { exerciseId: EXERCISE_ID, targetSets: 3, targetReps: null, restSeconds: 60 },
          { exerciseId: EXERCISE_ID, targetSets: 5, targetReps: null, restSeconds: 90 },
        ],
      }),
    );
    expect(result.success).toBe(true);
  });
});

describe("CustomExercisePayloadSchema", () => {
  function baseExercise(overrides: Record<string, unknown> = {}) {
    return {
      name: "Zottman curl",
      primaryMuscle: "biceps",
      secondaryMuscles: ["forearms"],
      equipment: "dumbbell",
      ...overrides,
    };
  }

  it("given a valid payload, when parsed, then it is accepted", () => {
    expect(CustomExercisePayloadSchema.safeParse(baseExercise()).success).toBe(true);
  });

  it("given secondaryMuscles including primaryMuscle, when parsed, then it is rejected", () => {
    expect(
      CustomExercisePayloadSchema.safeParse(baseExercise({ secondaryMuscles: ["biceps"] })).success,
    ).toBe(false);
  });

  it("given duplicate secondaryMuscles, when parsed, then it is rejected", () => {
    expect(
      CustomExercisePayloadSchema.safeParse(
        baseExercise({ secondaryMuscles: ["forearms", "forearms"] }),
      ).success,
    ).toBe(false);
  });

  it("given an unknown muscle value, when parsed, then it is rejected", () => {
    expect(
      CustomExercisePayloadSchema.safeParse(baseExercise({ primaryMuscle: "lats" })).success,
    ).toBe(false);
  });

  it("given license/author fields (pull-only), when parsed as a push payload, then it is rejected (strict object)", () => {
    expect(CustomExercisePayloadSchema.safeParse(baseExercise({ license: null })).success).toBe(
      false,
    );
  });
});

describe("PushRequestSchema envelope rules", () => {
  function change(overrides: Record<string, unknown> = {}) {
    return { type: "template", id: TEMPLATE_ID, deleted: true, ...overrides };
  }

  it("given zero changes, when parsed, then it is rejected", () => {
    expect(PushRequestSchema.safeParse({ changes: [] }).success).toBe(false);
  });

  it(`given ${PUSH_MAX_CHANGES + 1} changes, when parsed, then it is rejected`, () => {
    const changes = Array.from({ length: PUSH_MAX_CHANGES + 1 }, (_, index) =>
      change({ id: `b6e6b6a0-4444-4a22-8a33-${String(index).padStart(12, "0")}` }),
    );
    expect(PushRequestSchema.safeParse({ changes }).success).toBe(false);
  });

  it(`given exactly ${PUSH_MAX_CHANGES} changes, when parsed, then it is accepted`, () => {
    const changes = Array.from({ length: PUSH_MAX_CHANGES }, (_, index) =>
      change({ id: `b6e6b6a0-4444-4a22-8a33-${String(index).padStart(12, "0")}` }),
    );
    expect(PushRequestSchema.safeParse({ changes }).success).toBe(true);
  });

  it("given duplicate (type, id) pairs, when parsed, then it is rejected", () => {
    expect(PushRequestSchema.safeParse({ changes: [change(), change()] }).success).toBe(false);
  });

  it("given the same id with different types, when parsed, then it is accepted", () => {
    expect(
      PushRequestSchema.safeParse({
        changes: [change({ type: "template" }), change({ type: "workout" })],
      }).success,
    ).toBe(true);
  });

  it("given deleted: true with a payload, when parsed, then it is rejected", () => {
    expect(
      PushRequestSchema.safeParse({ changes: [change({ deleted: true, payload: {} })] }).success,
    ).toBe(false);
  });

  it("given deleted: true with a null payload, when parsed, then it is accepted", () => {
    expect(
      PushRequestSchema.safeParse({ changes: [change({ deleted: true, payload: null })] }).success,
    ).toBe(true);
  });

  it("given deleted: false with no payload, when parsed, then it is rejected", () => {
    expect(PushRequestSchema.safeParse({ changes: [change({ deleted: false })] }).success).toBe(
      false,
    );
  });

  it("given deleted: false with a non-object payload, when parsed, then it is rejected", () => {
    expect(
      PushRequestSchema.safeParse({ changes: [change({ deleted: false, payload: "nope" })] })
        .success,
    ).toBe(false);
  });

  it("given deleted: false with a malformed but object-shaped payload, when parsed, then the envelope still accepts it (per-item validation happens in the service)", () => {
    expect(
      PushRequestSchema.safeParse({
        changes: [change({ deleted: false, payload: { whatever: "goes" } })],
      }).success,
    ).toBe(true);
  });

  it("given an invalid uuid id, when parsed, then it is rejected", () => {
    expect(PushRequestSchema.safeParse({ changes: [change({ id: "not-a-uuid" })] }).success).toBe(
      false,
    );
  });

  it("given an unknown type, when parsed, then it is rejected", () => {
    expect(PushRequestSchema.safeParse({ changes: [change({ type: "unknown" })] }).success).toBe(
      false,
    );
  });

  it("given an unknown top-level key on a change, when parsed, then it is rejected", () => {
    expect(PushRequestSchema.safeParse({ changes: [change({ extra: "nope" })] }).success).toBe(
      false,
    );
  });

  it("given an unknown top-level key on the envelope itself, when parsed, then it is rejected (strict object)", () => {
    expect(PushRequestSchema.safeParse({ changes: [change()], extra: "nope" }).success).toBe(false);
  });
});
