import { describe, expect, it } from "vitest";
import { createArgon2PasswordHasher } from "./password-hasher.ts";

describe("createArgon2PasswordHasher", () => {
  it("given a password, when hashed, then the PHC string has the OWASP first-recommendation parameters", async () => {
    const hasher = createArgon2PasswordHasher();

    const hash = await hasher.hash("correct horse battery staple");

    expect(hash).toMatch(/^\$argon2id\$v=19\$m=19456,t=2,p=1\$/);
  });

  it("given a hash and its original password, when verified, then it returns true", async () => {
    const hasher = createArgon2PasswordHasher();
    const hash = await hasher.hash("correct horse battery staple");

    await expect(hasher.verify(hash, "correct horse battery staple")).resolves.toBe(true);
  });

  it("given a hash and a wrong password, when verified, then it returns false", async () => {
    const hasher = createArgon2PasswordHasher();
    const hash = await hasher.hash("correct horse battery staple");

    await expect(hasher.verify(hash, "wrong password")).resolves.toBe(false);
  });

  it("given the same password hashed twice, when compared, then the hashes differ (random salt)", async () => {
    const hasher = createArgon2PasswordHasher();

    const [first, second] = await Promise.all([
      hasher.hash("correct horse battery staple"),
      hasher.hash("correct horse battery staple"),
    ]);

    expect(first).not.toBe(second);
  });
});
