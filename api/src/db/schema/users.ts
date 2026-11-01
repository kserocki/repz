import { sql } from "drizzle-orm";
import { check, foreignKey, index, pgTable, text, uniqueIndex, uuid } from "drizzle-orm/pg-core";
import { timestamp } from "./sync.ts";
import { inVocabulary, UNIT_SYSTEMS } from "./vocabularies.ts";

/**
 * Not a sync root: the server is the only writer (auth, profile), so there is no `version`
 * to reconcile against a client-side copy.
 */
export const users = pgTable(
  "users",
  {
    id: uuid("id").primaryKey().default(sql`uuidv7()`),
    email: text("email").notNull(),
    passwordHash: text("password_hash").notNull(),
    displayName: text("display_name").notNull(),
    unitSystem: text("unit_system").notNull().default("kg"),
    createdAt: timestamp("created_at").notNull().defaultNow(),
    updatedAt: timestamp("updated_at").notNull().defaultNow(),
  },
  (t) => [
    uniqueIndex("users_email_lower_key").on(sql`lower(${t.email})`),
    check("users_unit_system_check", inVocabulary(t.unitSystem, UNIT_SYSTEMS)),
  ],
);

export const refreshTokens = pgTable(
  "refresh_tokens",
  {
    id: uuid("id").primaryKey().default(sql`uuidv7()`),
    userId: uuid("user_id").notNull(),
    familyId: uuid("family_id").notNull(),
    tokenHash: text("token_hash").notNull(),
    createdAt: timestamp("created_at").notNull().defaultNow(),
    expiresAt: timestamp("expires_at").notNull(),
    revokedAt: timestamp("revoked_at"),
  },
  (t) => [
    foreignKey({
      name: "refresh_tokens_user_id_fkey",
      columns: [t.userId],
      foreignColumns: [users.id],
    }).onDelete("cascade"),
    uniqueIndex("refresh_tokens_token_hash_key").on(t.tokenHash),
    index("refresh_tokens_user_id_idx").on(t.userId),
    index("refresh_tokens_family_id_idx").on(t.familyId),
  ],
);
