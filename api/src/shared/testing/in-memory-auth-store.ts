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
 * DB-free `AuthTransactions` fake: `run` executes synchronously (no real concurrency, since
 * every unit test is single-threaded), so it provides no row locking. The "row locked by a
 * concurrent refresh" and parallel-race behaviour is Spotter's job against real Postgres.
 */
export function createInMemoryAuthStore(
  generateId: () => string = () => crypto.randomUUID(),
): InMemoryAuthStore {
  const users = createInMemoryUsersRepository(generateId);
  const tokensById = new Map<string, StoredToken>();

  function toRecord({ tokenHash: _tokenHash, ...record }: StoredToken): RefreshTokenRecord {
    return record;
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

    async findByHashForUpdate(tokenHash) {
      const found = [...tokensById.values()].find((token) => token.tokenHash === tokenHash);
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
    run: (work) => work({ users, refreshTokens }),
  };

  return {
    users,
    refreshTokens,
    transactions,
    listRefreshTokens: () => [...tokensById.values()].map(toRecord),
  };
}
