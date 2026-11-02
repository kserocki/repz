import { describe, expect, it } from "vitest";
import type { SnapshotFile } from "../../db/seed/snapshot.schema.ts";
import { renderAttributionMarkdown } from "./render-attribution.ts";

function snapshot(): SnapshotFile {
  return {
    source: "https://wger.de/api/v2/exerciseinfo/",
    exercises: [
      {
        id: "b6e6b6a0-0000-4000-8000-000000000001",
        primaryMuscle: "chest",
        secondaryMuscles: ["triceps"],
        equipment: "barbell",
        license: "CC-BY-SA 3.0",
        author: "sistab2",
        source: { wgerId: 73, wgerUuid: "uuid-73", url: "https://wger.de/exercise/73/view-base" },
        translations: [{ locale: "en", name: "Bench Press", instructions: "Lay down." }],
      },
    ],
  };
}

describe("renderAttributionMarkdown", () => {
  it("given a snapshot, when rendered, then it states the compilation's own licence", () => {
    const markdown = renderAttributionMarkdown(snapshot());

    expect(markdown).toContain("wger.de");
    expect(markdown).toContain("CC BY-SA 4.0");
    expect(markdown).toContain("https://creativecommons.org/licenses/by-sa/4.0/");
  });

  it("given a snapshot, when rendered, then every exercise gets one credit row with its EN name, author, licence and source link", () => {
    const markdown = renderAttributionMarkdown(snapshot());

    expect(markdown).toContain("Bench Press");
    expect(markdown).toContain("sistab2");
    expect(markdown).toContain("[CC-BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/)");
    expect(markdown).toContain("[wger.de](https://wger.de/exercise/73/view-base)");
    expect(markdown).toContain("modified");
  });

  it("given an exercise with no EN translation name, when rendered, then it falls back to the exercise id", () => {
    const withoutEn = snapshot();
    const [exercise] = withoutEn.exercises;
    if (!exercise) {
      throw new Error("test fixture has no exercises");
    }
    withoutEn.exercises = [{ ...exercise, translations: [] }];

    const markdown = renderAttributionMarkdown(withoutEn);

    expect(markdown).toContain("b6e6b6a0-0000-4000-8000-000000000001");
  });
});
