import { eq, sql } from "drizzle-orm";
import postgres from "postgres";
import type { DatabaseExecutor } from "../../db/client.ts";
import { users } from "../../db/schema/index.ts";
import type { UnitSystem } from "../../db/schema/vocabularies.ts";
import { EmailTakenError } from "../auth/auth.errors.ts";

export type UserRecord = {
  id: string;
  email: string;
  passwordHash: string;
  displayName: string;
  unitSystem: UnitSystem;
};

export type NewUser = { email: string; passwordHash: string; displayName: string };
export type ProfileChanges = { displayName?: string; unitSystem?: UnitSystem };

export type UsersRepository = {
  insert(user: NewUser): Promise<UserRecord>;
  findByEmail(email: string): Promise<UserRecord | undefined>;
  findById(id: string): Promise<UserRecord | undefined>;
  updateProfile(id: string, changes: ProfileChanges): Promise<UserRecord | undefined>;
};

const EMAIL_UNIQUE_VIOLATION = "23505";
const EMAIL_UNIQUE_CONSTRAINT = "users_email_lower_key";

function toUserRecord(row: typeof users.$inferSelect): UserRecord {
  return {
    id: row.id,
    email: row.email,
    passwordHash: row.passwordHash,
    displayName: row.displayName,
    unitSystem: row.unitSystem,
  };
}

/** drizzle-orm wraps every driver error in `DrizzleQueryError`, with the original on `.cause`. */
function isEmailUniqueViolation(error: unknown): boolean {
  const cause = error instanceof Error ? (error.cause ?? error) : error;
  return (
    cause instanceof postgres.PostgresError &&
    cause.code === EMAIL_UNIQUE_VIOLATION &&
    cause.constraint_name === EMAIL_UNIQUE_CONSTRAINT
  );
}

/** Drizzle queries only, no business rules (routes → service → repository). */
export function createUsersRepository(db: DatabaseExecutor): UsersRepository {
  return {
    async insert(user) {
      try {
        const [row] = await db.insert(users).values(user).returning();
        if (!row) {
          throw new Error("Insert into users returned no row");
        }
        return toUserRecord(row);
      } catch (error) {
        if (isEmailUniqueViolation(error)) {
          throw new EmailTakenError();
        }
        throw error;
      }
    },

    async findByEmail(email) {
      const [row] = await db
        .select()
        .from(users)
        .where(sql`lower(${users.email}) = lower(${email})`)
        .limit(1);
      return row ? toUserRecord(row) : undefined;
    },

    async findById(id) {
      const [row] = await db.select().from(users).where(eq(users.id, id)).limit(1);
      return row ? toUserRecord(row) : undefined;
    },

    async updateProfile(id, changes) {
      const [row] = await db
        .update(users)
        .set({ ...changes, updatedAt: sql`now()` })
        .where(eq(users.id, id))
        .returning();
      return row ? toUserRecord(row) : undefined;
    },
  };
}
