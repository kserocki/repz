import { sql } from "drizzle-orm";
import { check, foreignKey, index, pgTable, primaryKey, text, uuid } from "drizzle-orm/pg-core";
import { syncColumns } from "./sync.ts";
import { users } from "./users.ts";
import { allInVocabulary, EQUIPMENT, inVocabulary, MUSCLES } from "./vocabularies.ts";

/**
 * Sync root. `owner_id null` = global catalog row (seeded by the catalog seeder), set = a
 * user's custom exercise. `isCustom` and `imageUrl` are derived at the API edge, never stored
 * (ADR-0019). `source_url` is the wger page an entry was curated from; null for custom rows.
 */
export const exercises = pgTable(
  "exercises",
  {
    id: uuid("id").primaryKey(),
    ownerId: uuid("owner_id"),
    name: text("name").notNull(),
    primaryMuscle: text("primary_muscle").notNull(),
    secondaryMuscles: text("secondary_muscles").array().notNull().default([]),
    equipment: text("equipment").notNull(),
    license: text("license"),
    author: text("author"),
    sourceUrl: text("source_url"),
    ...syncColumns(),
  },
  (t) => [
    foreignKey({
      name: "exercises_owner_id_fkey",
      columns: [t.ownerId],
      foreignColumns: [users.id],
    }).onDelete("cascade"),
    check("exercises_equipment_check", inVocabulary(t.equipment, EQUIPMENT)),
    check("exercises_primary_muscle_check", inVocabulary(t.primaryMuscle, MUSCLES)),
    check("exercises_secondary_muscles_check", allInVocabulary(t.secondaryMuscles, MUSCLES)),
    index("exercises_owner_id_version_idx").on(t.ownerId, t.version),
    index("exercises_catalog_version_idx").on(t.version).where(sql`${t.ownerId} is null`),
  ],
);

/**
 * Localised name + instructions, one row per `(exercise, locale)`. No `CHECK` on
 * `locale`: a vocabulary there would make adding a language a migration, which breaks the
 * "new language = new rows, no code change" rule (AGENTS.md). The tag format is validated by
 * Zod's `LocaleSchema` at the API edge. Child of the `exercises` root: cascades on delete, and
 * every write to the root replaces its full set of translations (never a partial patch).
 */
export const exerciseTranslations = pgTable(
  "exercise_translations",
  {
    exerciseId: uuid("exercise_id").notNull(),
    locale: text("locale").notNull(),
    name: text("name").notNull(),
    instructions: text("instructions"),
  },
  (t) => [
    primaryKey({ name: "exercise_translations_pkey", columns: [t.exerciseId, t.locale] }),
    foreignKey({
      name: "exercise_translations_exercise_id_fkey",
      columns: [t.exerciseId],
      foreignColumns: [exercises.id],
    }).onDelete("cascade"),
  ],
);
