import { z } from "@hono/zod-openapi";
import { EQUIPMENT, MUSCLES, type Muscle, SET_TYPES } from "../../db/schema/vocabularies.ts";
import type { FieldError } from "./sync.types.ts";
import { SYNC_TYPES } from "./sync.types.ts";

// --- Limits (Q3 defaults) ---------------------------------------------------------------------

export const NAME_MAX = 100;
export const NOTES_MAX = 2000;
export const WORKOUT_EXERCISES_MAX = 50;
export const SETS_PER_EXERCISE_MAX = 50;
export const TEMPLATE_EXERCISES_MAX = 50;
export const TARGET_SETS_MAX = 20;
export const REPS_MAX = 999;
export const WEIGHT_GRAMS_MAX = 1_000_000;
export const REST_SECONDS_MAX = 3600;
export const PUSH_MAX_CHANGES = 100;
export const PUSH_BODY_MAX_BYTES = 1 * 1024 * 1024;
export const PULL_MAX_LIMIT = 500;
export const PULL_DEFAULT_LIMIT = 500;

// --- Shared field schemas ----------------------------------------------------------------------

/** Accepts any offset (clients may be in any timezone); the server always answers with `…Z`. */
const IsoDateTimeSchema = z.iso.datetime({ offset: true });

const nameField = z.string().trim().min(1).max(NAME_MAX);
const notesField = z.string().max(NOTES_MAX).default("");
const MuscleSchema = z.enum(MUSCLES);
const EquipmentSchema = z.enum(EQUIPMENT);

const rpeField = z
  .number()
  .min(6)
  .max(10)
  .refine((value) => Number.isInteger(value * 2), {
    message: "rpe must be between 6 and 10 in steps of 0.5",
  })
  .nullable();

// --- Workout payload ---------------------------------------------------------------------------

const WorkoutSetPayloadSchema = z
  .strictObject({
    id: z.uuid(),
    type: z.enum(SET_TYPES),
    weightGrams: z.number().int().min(0).max(WEIGHT_GRAMS_MAX),
    reps: z.number().int().min(0).max(REPS_MAX),
    rpe: rpeField,
    completedAt: IsoDateTimeSchema.nullable(),
  })
  .openapi("WorkoutSetPayload", {
    example: {
      id: "b6e6b6a0-2222-4a22-8a33-000000000003",
      type: "normal",
      weightGrams: 60_000,
      reps: 8,
      rpe: 8,
      completedAt: "2026-11-01T10:05:00.000Z",
    },
  });

const WorkoutExercisePayloadSchema = z
  .strictObject({
    id: z.uuid(),
    exerciseId: z.uuid(),
    notes: notesField,
    sets: z.array(WorkoutSetPayloadSchema).max(SETS_PER_EXERCISE_MAX),
  })
  .openapi("WorkoutExercisePayload", {
    example: {
      id: "b6e6b6a0-2222-4a22-8a33-000000000002",
      exerciseId: "b6e6b6a0-3333-4a22-8a33-000000000001",
      notes: "",
      sets: [
        {
          id: "b6e6b6a0-2222-4a22-8a33-000000000003",
          type: "normal",
          weightGrams: 60_000,
          reps: 8,
          rpe: 8,
          completedAt: "2026-11-01T10:05:00.000Z",
        },
      ],
    },
  });

export const WorkoutPayloadSchema = z
  .strictObject({
    templateId: z.uuid().nullable(),
    name: nameField,
    notes: notesField,
    startedAt: IsoDateTimeSchema,
    finishedAt: IsoDateTimeSchema,
    exercises: z.array(WorkoutExercisePayloadSchema).max(WORKOUT_EXERCISES_MAX),
  })
  .refine(
    (workout) => new Date(workout.finishedAt).getTime() >= new Date(workout.startedAt).getTime(),
    {
      message: "finishedAt must be at or after startedAt",
      path: ["finishedAt"],
    },
  )
  .openapi("WorkoutPayload", {
    example: {
      templateId: "b6e6b6a0-4444-4a22-8a33-000000000001",
      name: "Push day",
      notes: "",
      startedAt: "2026-11-01T10:00:00.000Z",
      finishedAt: "2026-11-01T10:45:00.000Z",
      exercises: [
        {
          id: "b6e6b6a0-2222-4a22-8a33-000000000002",
          exerciseId: "b6e6b6a0-3333-4a22-8a33-000000000001",
          notes: "",
          sets: [
            {
              id: "b6e6b6a0-2222-4a22-8a33-000000000003",
              type: "normal",
              weightGrams: 60_000,
              reps: 8,
              rpe: 8,
              completedAt: "2026-11-01T10:05:00.000Z",
            },
          ],
        },
      ],
    },
  });

export type WorkoutPayload = z.infer<typeof WorkoutPayloadSchema>;

// --- Template payload ----------------------------------------------------------------------------

const TemplateExercisePayloadSchema = z
  .strictObject({
    exerciseId: z.uuid(),
    targetSets: z.number().int().min(1).max(TARGET_SETS_MAX),
    targetReps: z
      .strictObject({
        min: z.number().int().min(1).max(REPS_MAX),
        max: z.number().int().min(1).max(REPS_MAX),
      })
      .refine((range) => range.min <= range.max, {
        message: "min must be less than or equal to max",
      })
      .nullable(),
    restSeconds: z.number().int().min(0).max(REST_SECONDS_MAX),
  })
  .openapi("TemplateExercisePayload", {
    example: {
      exerciseId: "b6e6b6a0-3333-4a22-8a33-000000000001",
      targetSets: 3,
      targetReps: { min: 8, max: 12 },
      restSeconds: 90,
    },
  });

export const TemplatePayloadSchema = z
  .strictObject({
    name: nameField,
    notes: notesField,
    exercises: z.array(TemplateExercisePayloadSchema).max(TEMPLATE_EXERCISES_MAX),
  })
  .openapi("TemplatePayload", {
    example: {
      name: "Push day",
      notes: "",
      exercises: [
        {
          exerciseId: "b6e6b6a0-3333-4a22-8a33-000000000001",
          targetSets: 3,
          targetReps: { min: 8, max: 12 },
          restSeconds: 90,
        },
      ],
    },
  });

export type TemplatePayload = z.infer<typeof TemplatePayloadSchema>;

// --- Exercise payload ----------------------------------------------------------------------------

/** Shared by push (`CustomExercisePayload`) and pull (`ExercisePayload`); kept unrefined so `ExercisePayloadSchema` can `.extend()` it. */
const CustomExerciseShapeSchema = z.strictObject({
  name: nameField,
  primaryMuscle: MuscleSchema,
  secondaryMuscles: z.array(MuscleSchema).max(MUSCLES.length - 1),
  equipment: EquipmentSchema,
});

function validateMuscles(
  exercise: { primaryMuscle: Muscle; secondaryMuscles: Muscle[] },
  ctx: z.core.$RefinementCtx<{ primaryMuscle: Muscle; secondaryMuscles: Muscle[] }>,
): void {
  if (exercise.secondaryMuscles.includes(exercise.primaryMuscle)) {
    ctx.addIssue({
      code: "custom",
      path: ["secondaryMuscles"],
      message: "secondaryMuscles must not include primaryMuscle",
    });
  }
  if (new Set(exercise.secondaryMuscles).size !== exercise.secondaryMuscles.length) {
    ctx.addIssue({
      code: "custom",
      path: ["secondaryMuscles"],
      message: "secondaryMuscles must not contain duplicates",
    });
  }
}

export const CustomExercisePayloadSchema = CustomExerciseShapeSchema.superRefine(
  validateMuscles,
).openapi("CustomExercisePayload", {
  example: {
    name: "Zottman curl",
    primaryMuscle: "biceps",
    secondaryMuscles: ["forearms"],
    equipment: "dumbbell",
  },
});

export type CustomExercisePayload = z.infer<typeof CustomExercisePayloadSchema>;

export const ExercisePayloadSchema = CustomExerciseShapeSchema.extend({
  isCustom: z.boolean(),
  imageUrl: z.string().nullable(),
  license: z.string().nullable(),
  author: z.string().nullable(),
})
  .superRefine(validateMuscles)
  .openapi("ExercisePayload", {
    example: {
      name: "Bench press",
      primaryMuscle: "chest",
      secondaryMuscles: ["triceps", "shoulders"],
      equipment: "barbell",
      isCustom: false,
      imageUrl: "/static/exercises/b6e6b6a0-3333-4a22-8a33-000000000001.webp",
      license: "CC-BY-SA 4.0",
      author: "wger.de",
    },
  });

export type ExercisePayload = z.infer<typeof ExercisePayloadSchema>;

// --- Validation helper shared by every SyncWriter -----------------------------------------------

/** Parses `raw` against `schema`, mapping Zod issues to `FieldError[]` rooted at `payload`. */
export function parsePayloadWithSchema<T>(
  schema: z.ZodType<T>,
  raw: unknown,
): { ok: true; data: T } | { ok: false; errors: FieldError[] } {
  const result = schema.safeParse(raw);
  if (result.success) {
    return { ok: true, data: result.data };
  }
  const errors = result.error.issues.map((issue) => ({
    path: ["payload", ...issue.path.map(String)].join("."),
    message: issue.message,
  }));
  return { ok: false, errors };
}

// --- Push ----------------------------------------------------------------------------------------

/**
 * Documentation-only: the envelope's `payload` is `z.unknown()` at runtime (validated per item in
 * the service, see `parsePayloadWithSchema`), so one bad payload never fails the whole batch. The
 * three payload components are registered explicitly on the router (`sync.routes.ts`) so this
 * `oneOf` resolves even though no route ever uses these schemas as a literal request validator.
 */
const PushChangePayloadSchema = z
  .unknown()
  .optional()
  .openapi({
    description:
      "The aggregate payload. Required (and an object) when deleted is false; absent or null when deleted is true. Shape depends on type and is validated per item, never at the envelope level.",
    oneOf: [
      { $ref: "#/components/schemas/WorkoutPayload" },
      { $ref: "#/components/schemas/TemplatePayload" },
      { $ref: "#/components/schemas/CustomExercisePayload" },
    ],
  });

function isPayloadObject(payload: unknown): boolean {
  return typeof payload === "object" && payload !== null && !Array.isArray(payload);
}

const PushChangeSchema = z
  .strictObject({
    type: z.enum(SYNC_TYPES),
    id: z.uuid(),
    deleted: z.boolean(),
    payload: PushChangePayloadSchema,
  })
  .superRefine((change, ctx) => {
    if (change.deleted) {
      if (change.payload !== undefined && change.payload !== null) {
        ctx.addIssue({
          code: "custom",
          path: ["payload"],
          message: "A deleted change must not include a payload",
        });
      }
    } else if (!isPayloadObject(change.payload)) {
      ctx.addIssue({
        code: "custom",
        path: ["payload"],
        message: "payload is required and must be an object when deleted is false",
      });
    }
  });

export const PushRequestSchema = z
  .object({ changes: z.array(PushChangeSchema).min(1).max(PUSH_MAX_CHANGES) })
  .superRefine((body, ctx) => {
    const seen = new Set<string>();
    body.changes.forEach((change, index) => {
      const key = `${change.type}:${change.id}`;
      if (seen.has(key)) {
        ctx.addIssue({
          code: "custom",
          path: ["changes", index, "id"],
          message: `Duplicate change for ${change.type} ${change.id}`,
        });
      }
      seen.add(key);
    });
  })
  .openapi("PushRequest", {
    example: {
      changes: [
        {
          type: "workout",
          id: "b6e6b6a0-2222-4a22-8a33-000000000001",
          deleted: false,
          payload: {
            templateId: null,
            name: "Push day",
            notes: "",
            startedAt: "2026-11-01T10:00:00.000Z",
            finishedAt: "2026-11-01T10:45:00.000Z",
            exercises: [],
          },
        },
        { type: "template", id: "b6e6b6a0-4444-4a22-8a33-000000000001", deleted: true },
      ],
    },
  });

export type PushRequest = z.infer<typeof PushRequestSchema>;

export const ItemProblemSchema = z
  .object({
    type: z.string(),
    title: z.string(),
    errors: z.array(z.object({ path: z.string(), message: z.string() })).optional(),
  })
  .openapi("ItemProblem", {
    example: { type: "urn:repz:problem:read-only", title: "Read Only" },
  });

const AcceptedChangeSchema = z
  .object({
    type: z.enum(SYNC_TYPES),
    id: z.uuid(),
    version: z.number().int().nullable(),
    deleted: z.boolean(),
  })
  .openapi("AcceptedChange", {
    example: {
      type: "workout",
      id: "b6e6b6a0-2222-4a22-8a33-000000000001",
      version: 1042,
      deleted: false,
    },
  });

const RejectedChangeSchema = z
  .object({ type: z.enum(SYNC_TYPES), id: z.uuid(), problem: ItemProblemSchema })
  .openapi("RejectedChange", {
    example: {
      type: "exercise",
      id: "b6e6b6a0-3333-4a22-8a33-000000000001",
      problem: { type: "urn:repz:problem:read-only", title: "Read Only" },
    },
  });

export const PushResponseSchema = z
  .object({ accepted: z.array(AcceptedChangeSchema), rejected: z.array(RejectedChangeSchema) })
  .openapi("PushResponse", {
    example: {
      accepted: [
        {
          type: "workout",
          id: "b6e6b6a0-2222-4a22-8a33-000000000001",
          version: 1042,
          deleted: false,
        },
      ],
      rejected: [],
    },
  });

// --- Pull ----------------------------------------------------------------------------------------

export const PullQuerySchema = z.object({
  cursor: z.coerce.number().int().min(0).default(0).openapi({ example: 0 }),
  limit: z.coerce
    .number()
    .int()
    .min(1)
    .max(PULL_MAX_LIMIT)
    .default(PULL_DEFAULT_LIMIT)
    .openapi({ example: 500 }),
});

export type PullQuery = z.infer<typeof PullQuerySchema>;

const SyncChangePayloadSchema = z
  .union([WorkoutPayloadSchema, TemplatePayloadSchema, ExercisePayloadSchema])
  .nullable();

export const SyncChangeSchema = z
  .object({
    type: z.enum(SYNC_TYPES),
    id: z.uuid(),
    version: z.number().int(),
    deleted: z.boolean(),
    updatedAt: IsoDateTimeSchema,
    payload: SyncChangePayloadSchema,
  })
  .openapi("SyncChange", {
    example: {
      type: "exercise",
      id: "b6e6b6a0-3333-4a22-8a33-000000000001",
      version: 3,
      deleted: false,
      updatedAt: "2026-11-01T10:00:00.000Z",
      payload: {
        name: "Bench press",
        primaryMuscle: "chest",
        secondaryMuscles: ["triceps", "shoulders"],
        equipment: "barbell",
        isCustom: false,
        imageUrl: "/static/exercises/b6e6b6a0-3333-4a22-8a33-000000000001.webp",
        license: "CC-BY-SA 4.0",
        author: "wger.de",
      },
    },
  });

const TotalEstimateSchema = z
  .object({
    total: z.number().int().min(0),
    workout: z.number().int().min(0),
    template: z.number().int().min(0),
    exercise: z.number().int().min(0),
  })
  .openapi("TotalEstimate", {
    example: { total: 1300, workout: 1000, template: 20, exercise: 280 },
  });

export const PullResponseSchema = z
  .object({
    changes: z.array(SyncChangeSchema),
    nextCursor: z.number().int(),
    hasMore: z.boolean(),
    totalEstimate: TotalEstimateSchema,
  })
  .openapi("PullResponse", {
    example: {
      changes: [],
      nextCursor: 7,
      hasMore: false,
      totalEstimate: { total: 1300, workout: 1000, template: 20, exercise: 280 },
    },
  });
