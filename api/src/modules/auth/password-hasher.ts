import { hash, verify } from "@node-rs/argon2";

export type PasswordHasher = {
  hash(password: string): Promise<string>;
  verify(hash: string, password: string): Promise<boolean>;
};

/**
 * OWASP Password Storage Cheat Sheet's first argon2id configuration (19 MiB, 2 passes, 1
 * thread). The algorithm is left to `@node-rs/argon2`'s default (argon2id): its `Algorithm`
 * export is an ambient `const enum`, which `erasableSyntaxOnly` can't import as a value.
 * `verify` reads the parameters back from the PHC string, so they can be raised later without
 * invalidating existing hashes.
 */
const ARGON2_OPTIONS = {
  memoryCost: 19456,
  timeCost: 2,
  parallelism: 1,
};

export function createArgon2PasswordHasher(): PasswordHasher {
  return {
    hash: (password) => hash(password, ARGON2_OPTIONS),
    verify: (hashed, password) => verify(hashed, password),
  };
}
