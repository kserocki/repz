import { sql } from "drizzle-orm";
import {
  check,
  foreignKey,
  index,
  integer,
  pgTable,
  primaryKey,
  text,
  uuid,
} from "drizzle-orm/pg-core";
import { syncColumns } from "./sync.ts";
import { users } from "./users.ts";

export const workoutTemplates = pgTable(
  "workout_templates",
  {
    id: uuid("id").primaryKey(),
    userId: uuid("user_id").notNull(),
    name: text("name").notNull(),
    notes: text("notes").notNull().default(""),
    ...syncColumns(),
  },
  (t) => [
    foreignKey({
      name: "workout_templates_user_id_fkey",
      columns: [t.userId],
      foreignColumns: [users.id],
    }).onDelete("cascade"),
    index("workout_templates_user_id_version_idx").on(t.userId, t.version),
  ],
);

/**
 * No `id` of its own (ARCHITECTURE §4): `(template_id, position)` is the primary key. No FK to
 * `exercise_id` (ADR-0019): the aggregate is the unit of sync, and a template can reference a
 * custom exercise that was created and deleted offline before ever reaching the server.
 */
export const templateExercises = pgTable(
  "template_exercises",
  {
    templateId: uuid("template_id").notNull(),
    position: integer("position").notNull(),
    exerciseId: uuid("exercise_id").notNull(),
    targetSets: integer("target_sets").notNull(),
    targetRepsMin: integer("target_reps_min"),
    targetRepsMax: integer("target_reps_max"),
    restSeconds: integer("rest_seconds").notNull(),
  },
  (t) => [
    primaryKey({ name: "template_exercises_pkey", columns: [t.templateId, t.position] }),
    foreignKey({
      name: "template_exercises_template_id_fkey",
      columns: [t.templateId],
      foreignColumns: [workoutTemplates.id],
    }).onDelete("cascade"),
    check("template_exercises_position_check", sql`${t.position} >= 0`),
    check("template_exercises_target_sets_check", sql`${t.targetSets} >= 1`),
    check(
      "template_exercises_target_reps_check",
      sql`(${t.targetRepsMin} is null and ${t.targetRepsMax} is null)
          or (${t.targetRepsMin} is not null and ${t.targetRepsMax} is not null
              and ${t.targetRepsMin} >= 1 and ${t.targetRepsMin} <= ${t.targetRepsMax})`,
    ),
    check("template_exercises_rest_seconds_check", sql`${t.restSeconds} >= 0`),
  ],
);
