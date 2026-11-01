import type {
  AuthTransactions,
  RefreshTokenRecord,
  RefreshTokensRepository,
} from "../../modules/auth/auth.repository.ts";
import type { UsersRepository } from "../../modules/users/users.repository.ts";
import { createInMemoryUsersRepository } from "./in-memory-users-repository.ts";

export type InMemoryAuthStore = {
  users: UsersRepository;
  refreshTokens: RefreshTokensRepository;
  transactions: AuthTransactions;
  /** Test-only: every refresh token row ever inserted, for asserting none (or how many) exist. */
  listRefreshTokens(): RefreshTokenRecord[];
};

type StoredToken = RefreshTokenRecord & { tokenHash: string };

/**
 * DB-free `AuthTransactions` fake. `transactions.run` models Postgres's advisory-lock family
 * serialization: `lockFamilyOfToken` resolves to a per-family async mutex that stays held until
 * the enclosing `run` settles, so two concurrent `run` calls touching the same family are
 * serialized the same way two Postgres transactions holding the same `pg_advisory_xact_lock`
 * key would be. Row-level `FOR UPDATE` locking itself is not modelled (every unit test is
 * single-threaded); that and the full parallel-race behaviour are Spotter's job against real
 * Postgres.
 */
export function createInMemoryAuthStore(
  generateId: () => string = () => crypto.randomUUID(),
): InMemoryAuthStore {
  const users = createInMemoryUsersRepository(generateId);
  const tokensById = new Map<string, StoredToken>();
  const familyLockTails = new Map<string, Promise<void>>();

  function toRecord({ tokenHash: _tokenHash, ...record }: StoredToken): RefreshTokenRecord {
    return record;
  }

  function findByTokenHash(tokenHash: string): StoredToken | undefined {
    return [...tokensById.values()].find((token) => token.tokenHash === tokenHash);
  }

  const refreshTokens: RefreshTokensRepository = {
    async insert(token) {
      const stored: StoredToken = {
        id: generateId(),
        userId: token.userId,
        familyId: token.familyId,
        tokenHash: token.tokenHash,
        expiresAt: token.expiresAt,
        revokedAt: null,
        replacedBy: null,
      };
      tokensById.set(stored.id, stored);
      return toRecord(stored);
    },

    async lockFamilyOfToken(tokenHash) {
      return findByTokenHash(tokenHash)?.familyId;
    },

    async findByHashForUpdate(tokenHash) {
      const found = findByTokenHash(tokenHash);
      return found ? toRecord(found) : undefined;
    },

    async findByIdForUpdate(id) {
      const found = tokensById.get(id);
      return found ? toRecord(found) : undefined;
    },

    async markRotated(id, at, replacedBy) {
      const found = tokensById.get(id);
      if (found) {
        found.revokedAt = at;
        found.replacedBy = replacedBy;
      }
    },

    async revokeFamily(familyId, at) {
      for (const token of tokensById.values()) {
        if (token.familyId === familyId && token.revokedAt === null) {
          token.revokedAt = at;
        }
      }
    },
  };

  const transactions: AuthTransactions = {
    async run(work) {
      // Each `run` gets its own `lockFamilyOfToken` bound to the releases it should hold until
      // it settles, chained onto whatever the previous holder of that family's lock queued.
      const releases: Array<() => void> = [];
      const txRefreshTokens: RefreshTokensRepository = {
        ...refreshTokens,
        async lockFamilyOfToken(tokenHash) {
          const found = findByTokenHash(tokenHash);
          if (!found) {
            return undefined;
          }
          const previous = familyLockTails.get(found.familyId) ?? Promise.resolve();
          let release!: () => void;
          const held = new Promise<void>((resolve) => {
            release = resolve;
          });
          familyLockTails.set(
            found.familyId,
            previous.then(() => held),
          );
          await previous;
          releases.push(release);
          return found.familyId;
        },
      };

      try {
        return await work({ users, refreshTokens: txRefreshTokens });
      } finally {
        releases.forEach((release) => {
          release();
        });
      }
    },
  };

  return {
    users,
    refreshTokens,
    transactions,
    listRefreshTokens: () => [...tokensById.values()].map(toRecord),
  };
}
