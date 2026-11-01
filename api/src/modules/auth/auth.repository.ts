import { and, eq, isNull } from "drizzle-orm";
import type { PostgresJsDatabase } from "drizzle-orm/postgres-js";
import type { DatabaseExecutor, DatabaseSchema } from "../../db/client.ts";
import { refreshTokens } from "../../db/schema/index.ts";
import { createUsersRepository, type UsersRepository } from "../users/users.repository.ts";

export type RefreshTokenRecord = {
  id: string;
  userId: string;
  familyId: string;
  expiresAt: Date;
  revokedAt: Date | null;
  replacedBy: string | null;
};

export type NewRefreshToken = {
  userId: string;
  familyId: string;
  tokenHash: string;
  expiresAt: Date;
};

export type RefreshTokensRepository = {
  insert(token: NewRefreshToken): Promise<RefreshTokenRecord>;
  findByHashForUpdate(tokenHash: string): Promise<RefreshTokenRecord | undefined>;
  findByIdForUpdate(id: string): Promise<RefreshTokenRecord | undefined>;
  markRotated(id: string, at: Date, replacedBy: string): Promise<void>;
  revokeFamily(familyId: string, at: Date): Promise<void>;
};

export type AuthTransactions = {
  run<T>(
    work: (stores: {
      users: UsersRepository;
      refreshTokens: RefreshTokensRepository;
    }) => Promise<T>,
  ): Promise<T>;
};

const RECORD_COLUMNS = {
  id: refreshTokens.id,
  userId: refreshTokens.userId,
  familyId: refreshTokens.familyId,
  expiresAt: refreshTokens.expiresAt,
  revokedAt: refreshTokens.revokedAt,
  replacedBy: refreshTokens.replacedBy,
};

/** Drizzle queries only; `auth.service.ts` owns every rotation/reuse rule. */
function createRefreshTokensRepository(db: DatabaseExecutor): RefreshTokensRepository {
  return {
    async insert(token) {
      const [row] = await db.insert(refreshTokens).values(token).returning(RECORD_COLUMNS);
      if (!row) {
        throw new Error("Insert into refresh_tokens returned no row");
      }
      return row;
    },

    async findByHashForUpdate(tokenHash) {
      const [row] = await db
        .select(RECORD_COLUMNS)
        .from(refreshTokens)
        .where(eq(refreshTokens.tokenHash, tokenHash))
        .for("update")
        .limit(1);
      return row;
    },

    async findByIdForUpdate(id) {
      const [row] = await db
        .select(RECORD_COLUMNS)
        .from(refreshTokens)
        .where(eq(refreshTokens.id, id))
        .for("update")
        .limit(1);
      return row;
    },

    async markRotated(id, at, replacedBy) {
      await db
        .update(refreshTokens)
        .set({ revokedAt: at, replacedBy })
        .where(eq(refreshTokens.id, id));
    },

    async revokeFamily(familyId, at) {
      await db
        .update(refreshTokens)
        .set({ revokedAt: at })
        .where(and(eq(refreshTokens.familyId, familyId), isNull(refreshTokens.revokedAt)));
    },
  };
}

/**
 * Wraps a Postgres transaction with `users`/`refreshTokens` repositories bound to its `tx`, so
 * register, login, refresh and logout see one atomic view of both tables.
 */
export function createAuthTransactions(db: PostgresJsDatabase<DatabaseSchema>): AuthTransactions {
  return {
    run(work) {
      return db.transaction((tx) =>
        work({
          users: createUsersRepository(tx),
          refreshTokens: createRefreshTokensRepository(tx),
        }),
      );
    },
  };
}
