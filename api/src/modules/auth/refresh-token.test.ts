import { describe, expect, it } from "vitest";
import { generateRefreshToken, hashRefreshToken } from "./refresh-token.ts";

describe("generateRefreshToken", () => {
  it("given a generated token, when inspected, then it is 43 base64url characters (256 random bits)", () => {
    const token = generateRefreshToken();

    expect(token).toHaveLength(43);
    expect(token).toMatch(/^[A-Za-z0-9_-]+$/);
  });

  it("given two generated tokens, when compared, then they differ", () => {
    expect(generateRefreshToken()).not.toBe(generateRefreshToken());
  });
});

describe("hashRefreshToken", () => {
  it("given a known input, when hashed, then it matches the known SHA-256 hex digest", () => {
    expect(hashRefreshToken("")).toBe(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    );
    expect(hashRefreshToken("abc")).toBe(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
    );
  });

  it("given the same token, when hashed twice, then it produces the same hash (deterministic)", () => {
    const token = generateRefreshToken();

    expect(hashRefreshToken(token)).toBe(hashRefreshToken(token));
  });

  it("given two different tokens, when hashed, then the hashes differ", () => {
    expect(hashRefreshToken(generateRefreshToken())).not.toBe(
      hashRefreshToken(generateRefreshToken()),
    );
  });
});
