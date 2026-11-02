import { describe, expect, it } from "vitest";
import type { CurationEntry, CurationFile } from "../../db/seed/curation.schema.ts";
import type { SnapshotFile } from "../../db/seed/snapshot.schema.ts";
import { renderReviewTable } from "./render-review.ts";

const EXERCISE_ID = "b6e6b6a0-0000-4000-8000-000000000001";

function snapshot(
  translations: SnapshotFile["exercises"][number]["translations"] = [
    { locale: "en", name: "Bench Press", instructions: "Lay down." },
  ],
): SnapshotFile {
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
        source: {
          wgerId: 73,
          wgerUuid: "uuid-73",
          url: "https://wger.de/exercise/73/view-base",
          originalName: "Benchpress Dumbbells",
        },
        translations,
      },
    ],
  };
}

function curationEntry(overrides: Partial<CurationEntry> = {}): CurationEntry {
  return {
    id: EXERCISE_ID,
    wgerId: 73,
    names: {},
    ...overrides,
  };
}

describe("renderReviewTable", () => {
  it("given one exercise, when rendered, then it is grouped under its primary muscle with its EN name shown", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };

    const markdown = renderReviewTable(snapshot(), curation);

    expect(markdown).toContain("## chest");
    expect(markdown).toContain("Bench Press");
  });

  it("given the summary line, when rendered, then it reports the total and per-group/equipment/licence counts", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };

    const markdown = renderReviewTable(snapshot(), curation);

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

    const markdown = renderReviewTable(snapshot(), curation);

    expect(markdown).toContain("primary muscle");
    expect(markdown).toContain("wger lists this incorrectly");
  });

  it("given an entry with no overrides, when rendered, then the overrides column (the last one) is a dash", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };

    const markdown = renderReviewTable(snapshot(), curation);
    const row = markdown.split("\n").find((line) => line.includes("Bench Press"));
    const cells = row?.split("|").map((cell) => cell.trim()) ?? [];

    expect(cells.at(-2)).toBe("—");
  });

  it("given a snapshot with no extra locale beyond en, when rendered, then the header has no extra name column", () => {
    const curation: CurationFile = { exercises: [curationEntry()] };

    const markdown = renderReviewTable(snapshot(), curation);

    expect(markdown).toContain("| EN name | Primary | Secondary |");
  });

  it("given a snapshot where a row carries an extra, arbitrary locale (de) beside en, when rendered, then a generic '<locale> name' column appears with no code change", () => {
    const withExtraLocale = snapshot([
      { locale: "en", name: "Bench Press", instructions: "Lay down." },
      { locale: "de", name: "Bankdrücken", instructions: null },
    ]);
    const curation: CurationFile = { exercises: [curationEntry({ names: { de: "Bankdrücken" } })] };

    const markdown = renderReviewTable(withExtraLocale, curation);

    expect(markdown).toContain("| EN name | de name | Primary |");
    expect(markdown).toContain("Bankdrücken");
  });
});
