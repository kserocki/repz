import { describe, expect, it } from "vitest";
import type { SnapshotFile } from "../../db/seed/snapshot.schema.ts";
import { renderAttributionMarkdown } from "./render-attribution.ts";

function snapshot(
  translations: SnapshotFile["exercises"][number]["translations"] = [
    { locale: "en", name: "Bench Press", instructions: "Lay down." },
  ],
): SnapshotFile {
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

describe("renderAttributionMarkdown header", () => {
  it("given a snapshot with only en, when rendered, then it says modified but not translated", () => {
    const markdown = renderAttributionMarkdown(snapshot());

    expect(markdown).toContain("have been modified by Repz");
    expect(markdown).not.toContain("translated");
  });

  it("given a snapshot where a row carries a non-English locale, when rendered, then it says modified and translated", () => {
    const withExtraLocale = snapshot([
      { locale: "en", name: "Bench Press", instructions: "Lay down." },
      { locale: "de", name: "Bankdrücken", instructions: null },
    ]);

    const markdown = renderAttributionMarkdown(withExtraLocale);

    expect(markdown).toContain("have been modified and translated by Repz");
  });

  it("given any snapshot, when rendered, then it states the compilation's own licence", () => {
    const markdown = renderAttributionMarkdown(snapshot());

    expect(markdown).toContain("wger.de");
    expect(markdown).toContain("CC BY-SA 4.0");
    expect(markdown).toContain("https://creativecommons.org/licenses/by-sa/4.0/");
  });
});

describe("renderAttributionMarkdown rows", () => {
  it("given a snapshot, when rendered, then every exercise gets one credit row with our name, wger's original title, author, licence and source link", () => {
    const markdown = renderAttributionMarkdown(snapshot());

    expect(markdown).toContain(
      "| Exercise | Original title | Author | Original licence | Source | |",
    );
    expect(markdown).toContain("Bench Press");
    expect(markdown).toContain("Benchpress Dumbbells");
    expect(markdown).toContain("sistab2");
    expect(markdown).toContain("[CC-BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/)");
    expect(markdown).toContain("[wger.de](https://wger.de/exercise/73/view-base)");
    expect(markdown).toContain("modified");
  });

  it("given an exercise with no EN translation name, when rendered, then our name column falls back to the exercise id", () => {
    const withoutEn = snapshot([]);

    const markdown = renderAttributionMarkdown(withoutEn);

    expect(markdown).toContain("b6e6b6a0-0000-4000-8000-000000000001");
  });
});
