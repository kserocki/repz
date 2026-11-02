import { describe, expect, it } from "vitest";
import { compareCodePoints } from "./codepoint-compare.ts";

describe("compareCodePoints", () => {
  it("given a before b by code point, when compared, then it returns a negative number", () => {
    expect(compareCodePoints("a", "b")).toBeLessThan(0);
  });

  it("given a after b by code point, when compared, then it returns a positive number", () => {
    expect(compareCodePoints("b", "a")).toBeGreaterThan(0);
  });

  it("given equal strings, when compared, then it returns zero", () => {
    expect(compareCodePoints("same", "same")).toBe(0);
  });

  it("given strings sorted by code point, when used to sort, then upper case sorts before lower case (unlike locale-aware sorting)", () => {
    expect(["b", "A", "a", "B"].sort(compareCodePoints)).toEqual(["A", "B", "a", "b"]);
  });

  it("given locale-sensitive characters, when sorted, then the order is code-point based, not locale based", () => {
    expect(["de", "en", "fr"].sort(compareCodePoints)).toEqual(["de", "en", "fr"]);
  });
});
