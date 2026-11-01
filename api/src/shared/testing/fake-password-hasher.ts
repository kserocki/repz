import type { PasswordHasher } from "../../modules/auth/password-hasher.ts";

const PREFIX = "fake-hash:";

/** Fast and deterministic: no real argon2 cost for tests that aren't exercising hashing itself. */
export function createFakePasswordHasher(): PasswordHasher {
  return {
    hash: async (password) => `${PREFIX}${password}`,
    verify: async (hash, password) => hash === `${PREFIX}${password}`,
  };
}
