import { sql } from "drizzle-orm";
import {
  bigint,
  check,
  foreignKey,
  index,
  integer,
  numeric,
  pgTable,
  text,
  uniqueIndex,
  uuid,
} from "drizzle-orm/pg-core";
import { syncColumns, timestamp } from "./sync.ts";
import { users } from "./users.ts";
import { inVocabulary, SET_TYPES } from "./vocabularies.ts";

/**
 * Sync root. The server only ever holds finished workouts (ARCHITECTURE §5.4: the active
 * workout is local-only until it finishes), hence `finished_at NOT NULL`. No FK on
 * `template_id` (ADR-0019): a workout can legitimately reference a template that was
 * created and deleted offline before ever reaching the server.
 */
export const workouts = pgTable(
  "workouts",
  {
    id: uuid("id").primaryKey(),
    userId: uuid("user_id").notNull(),
    templateId: uuid("template_id"),
    name: text("name").notNull(),
    notes: text("notes").notNull().default(""),
    startedAt: timestamp("started_at").notNull(),
    finishedAt: timestamp("finished_at").notNull(),
    ...syncColumns(),
  },
  (t) => [
    foreignKey({
      name: "workouts_user_id_fkey",
      columns: [t.userId],
      foreignColumns: [users.id],
    }).onDelete("cascade"),
    check("workouts_finished_at_check", sql`${t.finishedAt} >= ${t.startedAt}`),
    index("workouts_user_id_version_idx").on(t.userId, t.version),
  ],
);

/** No FK on `exercise_id` (ADR-0019), same reasoning as `workouts.template_id`. */
export const workoutExercises = pgTable(
  "workout_exercises",
  {
    id: uuid("id").primaryKey(),
    workoutId: uuid("workout_id").notNull(),
    exerciseId: uuid("exercise_id").notNull(),
    position: integer("position").notNull(),
    notes: text("notes").notNull().default(""),
  },
  (t) => [
    foreignKey({
      name: "workout_exercises_workout_id_fkey",
      columns: [t.workoutId],
      foreignColumns: [workouts.id],
    }).onDelete("cascade"),
    uniqueIndex("workout_exercises_workout_id_position_key").on(t.workoutId, t.position),
    check("workout_exercises_position_check", sql`${t.position} >= 0`),
  ],
);

export const workoutSets = pgTable(
  "workout_sets",
  {
    id: uuid("id").primaryKey(),
    workoutExerciseId: uuid("workout_exercise_id").notNull(),
    position: integer("position").notNull(),
    type: text("type").notNull(),
    weightGrams: bigint("weight_grams", { mode: "number" }).notNull(),
    reps: integer("reps").notNull(),
    rpe: numeric("rpe", { precision: 3, scale: 1, mode: "number" }),
    completedAt: timestamp("completed_at"),
  },
  (t) => [
    foreignKey({
      name: "workout_sets_workout_exercise_id_fkey",
      columns: [t.workoutExerciseId],
      foreignColumns: [workoutExercises.id],
    }).onDelete("cascade"),
    uniqueIndex("workout_sets_workout_exercise_id_position_key").on(
      t.workoutExerciseId,
      t.position,
    ),
    check("workout_sets_type_check", inVocabulary(t.type, SET_TYPES)),
    check("workout_sets_weight_grams_check", sql`${t.weightGrams} >= 0`),
    check("workout_sets_reps_check", sql`${t.reps} >= 0`),
    check("workout_sets_rpe_check", sql`${t.rpe} is null or ${t.rpe} between 1 and 10`),
    check("workout_sets_position_check", sql`${t.position} >= 0`),
  ],
);
