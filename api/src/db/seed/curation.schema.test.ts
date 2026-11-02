import { describe, expect, it } from "vitest";
import { CurationEntrySchema, CurationFileSchema } from "./curation.schema.ts";

const ID = "b6e6b6a0-0000-4000-8000-000000000001";

function baseEntry(overrides: Record<string, unknown> = {}) {
  return { id: ID, wgerId: 73, names: { en: "Bench Press" }, ...overrides };
}

describe("CurationEntrySchema names", () => {
  it("given no locale keys at all, when parsed, then it is accepted (no locale is required)", () => {
    expect(CurationEntrySchema.safeParse(baseEntry({ names: {} })).success).toBe(true);
  });

  it("given an arbitrary, non-English locale key, when parsed, then it is accepted", () => {
    expect(CurationEntrySchema.safeParse(baseEntry({ names: { de: "Bankdrücken" } })).success).toBe(
      true,
    );
  });

  it("given an invalid locale key, when parsed, then it is rejected", () => {
    expect(
      CurationEntrySchema.safeParse(baseEntry({ names: { ENGLISH: "Bench Press" } })).success,
    ).toBe(false);
  });
});

describe("CurationEntrySchema override reasons", () => {
  it("given an equipment override with no reviewNote, when parsed, then it is rejected", () => {
    const result = CurationEntrySchema.safeParse(baseEntry({ equipment: "other" }));

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((issue) => issue.path.join(".") === "reviewNote")).toBe(true);
    }
  });

  it("given an equipment override with a reviewNote, when parsed, then it is accepted", () => {
    const result = CurationEntrySchema.safeParse(
      baseEntry({ equipment: "other", reviewNote: "wger lists no equipment on this entry" }),
    );

    expect(result.success).toBe(true);
  });

  it("given a primaryMuscle override with no reviewNote, when parsed, then it is rejected", () => {
    expect(CurationEntrySchema.safeParse(baseEntry({ primaryMuscle: "chest" })).success).toBe(
      false,
    );
  });

  it("given a secondaryMuscles override with no reviewNote, when parsed, then it is rejected", () => {
    expect(
      CurationEntrySchema.safeParse(baseEntry({ secondaryMuscles: ["triceps"] })).success,
    ).toBe(false);
  });

  it("given a reviewNote that is only whitespace, when parsed, then it is still rejected", () => {
    expect(
      CurationEntrySchema.safeParse(baseEntry({ equipment: "other", reviewNote: "   " })).success,
    ).toBe(false);
  });

  it("given no override at all, when parsed, then no reviewNote is required", () => {
    expect(CurationEntrySchema.safeParse(baseEntry()).success).toBe(true);
  });

  it("given only an EN name override (routine Title Case rename), when parsed, then no reviewNote is required", () => {
    expect(CurationEntrySchema.safeParse(baseEntry({ names: { en: "Barbell Row" } })).success).toBe(
      true,
    );
  });
});

describe("CurationEntrySchema secondary/primary consistency", () => {
  it("given secondaryMuscles including primaryMuscle, when parsed, then it is rejected", () => {
    const result = CurationEntrySchema.safeParse(
      baseEntry({
        primaryMuscle: "chest",
        secondaryMuscles: ["chest"],
        reviewNote: "override",
      }),
    );

    expect(result.success).toBe(false);
  });

  it("given duplicate secondaryMuscles, when parsed, then it is rejected", () => {
    const result = CurationEntrySchema.safeParse(
      baseEntry({ secondaryMuscles: ["chest", "chest"], reviewNote: "override" }),
    );

    expect(result.success).toBe(false);
  });
});

describe("CurationFileSchema", () => {
  it("given duplicate ids, when parsed, then it is rejected", () => {
    const result = CurationFileSchema.safeParse({
      exercises: [baseEntry(), baseEntry({ wgerId: 184 })],
    });

    expect(result.success).toBe(false);
  });

  it("given duplicate wgerIds, when parsed, then it is rejected", () => {
    const result = CurationFileSchema.safeParse({
      exercises: [baseEntry(), baseEntry({ id: "b6e6b6a0-0000-4000-8000-000000000002" })],
    });

    expect(result.success).toBe(false);
  });

  it("given distinct ids and wgerIds, when parsed, then it is accepted", () => {
    const result = CurationFileSchema.safeParse({
      exercises: [
        baseEntry(),
        baseEntry({ id: "b6e6b6a0-0000-4000-8000-000000000002", wgerId: 184 }),
      ],
    });

    expect(result.success).toBe(true);
  });
});
