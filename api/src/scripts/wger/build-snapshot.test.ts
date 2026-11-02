import { describe, expect, it } from "vitest";
import {
  type CurationEntry,
  CurationEntrySchema,
  type CurationFile,
} from "../../db/seed/curation.schema.ts";
import { NAME_MAX } from "../../modules/sync/sync.schemas.ts";
import { buildSnapshot, WGER_SOURCE_URL } from "./build-snapshot.ts";
import type { WgerExerciseInfo } from "./wger.schemas.ts";

const BENCH_PRESS_ID = "b6e6b6a0-0000-4000-8000-000000000001";
const DEADLIFTS_ID = "b6e6b6a0-0000-4000-8000-000000000002";

function wgerExercise(overrides: Partial<WgerExerciseInfo> = {}): WgerExerciseInfo {
  return {
    id: 73,
    uuid: "3717d144-7815-4a97-9a56-956fb889c996",
    muscles: [{ id: 4 }],
    muscles_secondary: [{ id: 2 }, { id: 5 }],
    equipment: [{ id: 1 }],
    license: { short_name: "CC-BY-SA 3" },
    license_author: "sistab2",
    translations: [
      {
        language: 2,
        name: "Bench Press",
        description: "<p>Lay down on a bench.</p>",
        license: 1,
        license_author: "sistab2",
        author_history: ["sistab2"],
      },
    ],
    ...overrides,
  };
}

function curationEntry(overrides: Record<string, unknown> = {}): CurationEntry {
  return CurationEntrySchema.parse({
    id: BENCH_PRESS_ID,
    wgerId: 73,
    names: { pl: "Wyciskanie sztangi leżąc" },
    ...overrides,
  });
}

function curationFile(exercises: CurationEntry[]): CurationFile {
  return { exercises };
}

describe("buildSnapshot happy path", () => {
  it("given one clean curation entry, when built, then the snapshot has the derived fields in a fixed key order", () => {
    const result = buildSnapshot([wgerExercise()], curationFile([curationEntry()]));

    expect(result.ok).toBe(true);
    if (!result.ok) {
      return;
    }
    expect(result.snapshot).toEqual({
      source: WGER_SOURCE_URL,
      exercises: [
        {
          id: BENCH_PRESS_ID,
          primaryMuscle: "chest",
          secondaryMuscles: ["shoulders", "triceps"],
          equipment: "barbell",
          license: "CC-BY-SA 3.0",
          author: "sistab2",
          source: {
            wgerId: 73,
            wgerUuid: "3717d144-7815-4a97-9a56-956fb889c996",
            url: "https://wger.de/exercise/73/view-base",
          },
          translations: [
            { locale: "en", name: "Bench Press", instructions: "Lay down on a bench." },
            { locale: "pl", name: "Wyciskanie sztangi leżąc", instructions: null },
          ],
        },
      ],
    });
  });

  it("given an empty-equipment exercise with an equipment override, when built, then the override is used", () => {
    const wger = wgerExercise({ equipment: [] });
    const entry = curationEntry({ equipment: "machine" });

    const result = buildSnapshot([wger], curationFile([entry]));

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.snapshot.exercises[0]?.equipment).toBe("machine");
    }
  });
});

describe("buildSnapshot overrides win", () => {
  it("given primaryMuscle, secondaryMuscles, equipment, EN name and EN instructions overrides, when built, then every override is used verbatim, not the derived value", () => {
    // Without overrides this wger record would be ambiguous (chest + triceps primaries).
    const wger = wgerExercise({ muscles: [{ id: 4 }, { id: 5 }], muscles_secondary: [] });
    const entry = curationEntry({
      primaryMuscle: "triceps",
      secondaryMuscles: ["chest"],
      equipment: "dumbbell",
      names: { en: "Custom Bench Variant", pl: "Nazwa PL" },
      instructions: { en: "Custom rewritten instructions." },
    });

    const result = buildSnapshot([wger], curationFile([entry]));

    expect(result.ok).toBe(true);
    if (!result.ok) {
      return;
    }
    const exercise = result.snapshot.exercises[0];
    expect(exercise?.primaryMuscle).toBe("triceps");
    expect(exercise?.secondaryMuscles).toEqual(["chest"]);
    expect(exercise?.equipment).toBe("dumbbell");
    expect(exercise?.translations).toEqual([
      {
        locale: "en",
        name: "Custom Bench Variant",
        instructions: "Custom rewritten instructions.",
      },
      { locale: "pl", name: "Nazwa PL", instructions: null },
    ]);
  });
});

describe("buildSnapshot problems", () => {
  it("given a curation wgerId with no matching wger exercise, when built, then it is a problem", () => {
    const result = buildSnapshot(
      [wgerExercise({ id: 73 })],
      curationFile([curationEntry({ wgerId: 999 })]),
    );

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining("wger exercise 999 not found")]);
    }
  });

  it("given a wger exercise with no EN translation, when built, then it is a problem", () => {
    const wger = wgerExercise({
      translations: [
        {
          language: 14,
          name: "Jakieś ćwiczenie",
          description: "<p>Opis.</p>",
          license: 1,
          license_author: "x",
          author_history: [],
        },
      ],
    });

    const result = buildSnapshot([wger], curationFile([curationEntry()]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining("no EN translation")]);
    }
  });

  it("given wger primaries that map to two different groups with no override, when built, then it is an ambiguous-primary problem", () => {
    const wger = wgerExercise({ muscles: [{ id: 4 }, { id: 5 }], muscles_secondary: [] });

    const result = buildSnapshot([wger], curationFile([curationEntry()]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining("ambiguous primary muscle")]);
    }
  });

  it("given an unknown wger muscle id, when built, then it is a problem naming the id", () => {
    const wger = wgerExercise({ muscles: [{ id: 999 }] });

    const result = buildSnapshot([wger], curationFile([curationEntry()]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining("unknown wger muscle id 999")]);
    }
  });

  it("given empty equipment with no override, when built, then it is a problem", () => {
    const wger = wgerExercise({ equipment: [] });

    const result = buildSnapshot([wger], curationFile([curationEntry()]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining("equipment could not be derived")]);
    }
  });

  it("given an ODbL exercise license, when built, then it is an unknown-licence problem", () => {
    const wger = wgerExercise({ license: { short_name: "ODbL" } });

    const result = buildSnapshot([wger], curationFile([curationEntry()]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining("unknown or unsupported licence")]);
    }
  });

  it("given an EN description with an unknown HTML tag, when built, then it is a problem naming the tag", () => {
    const wger = wgerExercise({
      translations: [
        {
          language: 2,
          name: "Bench Press",
          description: "<p>Text</p><br>",
          license: 1,
          license_author: "sistab2",
          author_history: ["sistab2"],
        },
      ],
    });

    const result = buildSnapshot([wger], curationFile([curationEntry()]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining("unknown HTML tag <br>")]);
    }
  });

  it(`given an EN name over ${NAME_MAX} characters with no override, when built, then it is a problem`, () => {
    const wger = wgerExercise({
      translations: [
        {
          language: 2,
          name: "a".repeat(NAME_MAX + 1),
          description: "<p>Text</p>",
          license: 1,
          license_author: "sistab2",
          author_history: ["sistab2"],
        },
      ],
    });

    const result = buildSnapshot([wger], curationFile([curationEntry()]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toEqual([expect.stringContaining(`over ${NAME_MAX} characters`)]);
    }
  });

  it("given every problem kind at once, when built, then every problem is reported and nothing is written", () => {
    const wgerA = wgerExercise({ id: 73, muscles: [{ id: 999 }] });
    const entryA = curationEntry({ id: BENCH_PRESS_ID, wgerId: 73 });
    const entryB = curationEntry({ id: DEADLIFTS_ID, wgerId: 404, names: { pl: "Martwy ciąg" } });

    const result = buildSnapshot([wgerA], curationFile([entryA, entryB]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toHaveLength(2);
    }
  });
});

describe("buildSnapshot locale completeness", () => {
  it("given one entry with an extra locale the others lack, when built, then it is a problem naming that entry", () => {
    const wgerA = wgerExercise({ id: 73 });
    const wgerB = wgerExercise({ id: 184, uuid: "ee8e8db4-2d82-49e1-ab7f-891e9a354934" });
    const entryA = curationEntry({ id: BENCH_PRESS_ID, wgerId: 73, names: { pl: "A" } });
    const entryB = curationEntry({
      id: DEADLIFTS_ID,
      wgerId: 184,
      names: { pl: "B", fr: "C" },
    });

    const result = buildSnapshot([wgerA, wgerB], curationFile([entryA, entryB]));

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.problems).toHaveLength(1);
      expect(result.problems[0]).toContain(DEADLIFTS_ID);
      expect(result.problems[0]).toContain("do not match");
    }
  });

  it("given every entry with the same extra locales, when built, then there is no completeness problem", () => {
    const wgerA = wgerExercise({ id: 73 });
    const wgerB = wgerExercise({ id: 184, uuid: "ee8e8db4-2d82-49e1-ab7f-891e9a354934" });
    const entryA = curationEntry({ id: BENCH_PRESS_ID, wgerId: 73, names: { pl: "A" } });
    const entryB = curationEntry({ id: DEADLIFTS_ID, wgerId: 184, names: { pl: "B" } });

    const result = buildSnapshot([wgerA, wgerB], curationFile([entryA, entryB]));

    expect(result.ok).toBe(true);
  });
});

describe("buildSnapshot determinism", () => {
  it("given the same wger data in a different (shuffled) page order, when built, then the output is byte-identical", () => {
    const wgerA = wgerExercise({ id: 73 });
    const wgerB = wgerExercise({ id: 184, uuid: "ee8e8db4-2d82-49e1-ab7f-891e9a354934" });
    const wgerC = wgerExercise({ id: 9, uuid: "1b020b3a-3732-4c7e-92fd-a0cec90ed69b" });
    const entries = curationFile([
      curationEntry({ id: BENCH_PRESS_ID, wgerId: 73 }),
      curationEntry({ id: DEADLIFTS_ID, wgerId: 184, names: { pl: "Martwy ciąg" } }),
      curationEntry({
        id: "b6e6b6a0-0000-4000-8000-000000000003",
        wgerId: 9,
        names: { pl: "Wymach kettlebell" },
      }),
    ]);

    const inOrder = buildSnapshot([wgerA, wgerB, wgerC], entries);
    const shuffled = buildSnapshot([wgerC, wgerA, wgerB], entries);

    expect(inOrder.ok).toBe(true);
    expect(shuffled.ok).toBe(true);
    if (inOrder.ok && shuffled.ok) {
      expect(JSON.stringify(shuffled.snapshot)).toBe(JSON.stringify(inOrder.snapshot));
      expect(inOrder.snapshot.exercises.map((exercise) => exercise.id)).toEqual(
        [...inOrder.snapshot.exercises.map((exercise) => exercise.id)].sort((a, b) =>
          a.localeCompare(b),
        ),
      );
    }
  });
});
