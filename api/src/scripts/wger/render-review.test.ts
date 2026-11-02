import { describe, expect, it } from "vitest";
import type { CurationEntry, CurationFile } from "../../db/seed/curation.schema.ts";
import type { SnapshotFile } from "../../db/seed/snapshot.schema.ts";
import { renderReviewTable } from "./render-review.ts";
import type { WgerExerciseInfo } from "./wger.schemas.ts";

const EXERCISE_ID = "b6e6b6a0-0000-4000-8000-000000000001";

function snapshot(): SnapshotFile {
  return {
    source: "https://wger.de/api/v2/exerciseinfo/",
    exercises: [
      {
        id: EXERCISE_ID,
        primaryMuscle: "chest",
        secondaryMuscles: ["triceps"],
        equipment: "barbell",
        license: "CC-BY-SA 3.0",
        author: "sistab2",
        source: { wgerId: 73, wgerUuid: "uuid-73", url: "https://wger.de/exercise/73/view-base" },
        translations: [
          { locale: "en", name: "Bench Press", instructions: "Lay down." },
          { locale: "pl", name: "Wyciskanie sztangi leżąc", instructions: null },
        ],
      },
    ],
  };
}

function curationEntry(overrides: Partial<CurationEntry> = {}): CurationEntry {
  return {
    id: EXERCISE_ID,
    wgerId: 73,
    names: { pl: "Wyciskanie sztangi leżąc" },
    ...overrides,
  };
}

function wgerExercise(): WgerExerciseInfo {
  return {
    id: 73,
    uuid: "uuid-73",
    muscles: [{ id: 4 }],
    muscles_secondary: [],
    equipment: [{ id: 1 }],
    license: { short_name: "CC-BY-SA 3" },
    license_author: "sistab2",
    translations: [
      {
        language: 2,
        name: "Bench Press",
        description: "<p>Lay down.</p>",
        license: 1,
        license_author: "sistab2",
        author_history: ["sistab2"],
      },
    ],
  };
}

describe("renderReviewTable", () => {
  it("given one exercise, when rendered, then it is grouped under its primary muscle with both names shown", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };
    const wgerById = new Map([[73, wgerExercise()]]);

    const markdown = renderReviewTable(snapshot(), curation, wgerById);

    expect(markdown).toContain("## chest");
    expect(markdown).toContain("Bench Press");
    expect(markdown).toContain("Wyciskanie sztangi leżąc");
  });

  it("given the summary line, when rendered, then it reports the total and per-group/equipment/licence counts", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };
    const wgerById = new Map([[73, wgerExercise()]]);

    const markdown = renderReviewTable(snapshot(), curation, wgerById);

    expect(markdown).toContain("1 exercises");
    expect(markdown).toContain("chest 1");
    expect(markdown).toContain("barbell 1");
    expect(markdown).toContain("CC-BY-SA 3.0 1");
  });

  it("given an entry with overrides and a review note, when rendered, then the overrides column names them and the note", () => {
    const curation: CurationFile = {
      exercises: [
        curationEntry({
          primaryMuscle: "chest",
          reviewNote: "wger lists this incorrectly",
        }),
      ],
    };
    const wgerById = new Map([[73, wgerExercise()]]);

    const markdown = renderReviewTable(snapshot(), curation, wgerById);

    expect(markdown).toContain("primary muscle");
    expect(markdown).toContain("wger lists this incorrectly");
  });

  it("given an entry with no overrides, when rendered, then the overrides column is a dash", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };
    const wgerById = new Map([[73, wgerExercise()]]);

    const markdown = renderReviewTable(snapshot(), curation, wgerById);
    const row = markdown.split("\n").find((line) => line.includes("Bench Press"));
    const cells = row?.split("|").map((cell) => cell.trim()) ?? [];

    expect(cells[9]).toBe("—");
  });

  it("given wger has no Polish translation for an exercise, when rendered, then the wger PL name column is a dash", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };
    const wgerById = new Map([[73, wgerExercise()]]);

    const markdown = renderReviewTable(snapshot(), curation, wgerById);
    const row = markdown.split("\n").find((line) => line.includes("Bench Press"));
    const cells = row?.split("|").map((cell) => cell.trim()) ?? [];

    expect(cells[10]).toBe("—");
  });
});
