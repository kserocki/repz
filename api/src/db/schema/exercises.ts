import { sql } from "drizzle-orm";
import { check, foreignKey, index, pgTable, text, uuid } from "drizzle-orm/pg-core";
import { syncColumns } from "./sync.ts";
import { users } from "./users.ts";
import { EQUIPMENT, inVocabulary } from "./vocabularies.ts";

/**
 * Sync root. `owner_id null` = global catalog row (seeded by the catalog seeder), set = a
 * user's custom exercise. `isCustom` and `imageUrl` are derived at the API edge, never stored
 * (ADR-0019).
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
    ...syncColumns(),
  },
  (t) => [
    foreignKey({
      name: "exercises_owner_id_fkey",
      columns: [t.ownerId],
      foreignColumns: [users.id],
    }).onDelete("cascade"),
    check("exercises_equipment_check", inVocabulary(t.equipment, EQUIPMENT)),
    index("exercises_owner_id_version_idx").on(t.ownerId, t.version),
    index("exercises_catalog_version_idx").on(t.version).where(sql`${t.ownerId} is null`),
  ],
);
