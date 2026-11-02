import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { CurationFileSchema } from "./curation.schema.ts";
import { SnapshotFileSchema } from "./snapshot.schema.ts";

const SNAPSHOT_PATH = `${import.meta.dirname}/exercises.json`;
const CURATION_PATH = `${import.meta.dirname}/curation.json`;
const ATTRIBUTION_PATH = `${import.meta.dirname}/ATTRIBUTION.md`;

function loadSnapshot() {
  const raw: unknown = JSON.parse(readFileSync(SNAPSHOT_PATH, "utf8"));
  return SnapshotFileSchema.parse(raw);
}

function loadCuration() {
  const raw: unknown = JSON.parse(readFileSync(CURATION_PATH, "utf8"));
  return CurationFileSchema.parse(raw);
}

describe("the committed exercises.json", () => {
  it("parses against SnapshotFileSchema", () => {
    expect(() => loadSnapshot()).not.toThrow();
  });

  it("has unique exercise ids", () => {
    const snapshot = loadSnapshot();
    const ids = snapshot.exercises.map((exercise) => exercise.id);

    expect(new Set(ids).size).toBe(ids.length);
  });

  it("gives every exercise an en translation", () => {
    const snapshot = loadSnapshot();

    for (const exercise of snapshot.exercises) {
      expect(exercise.translations.some((translation) => translation.locale === "en")).toBe(true);
    }
  });

  it("gives every exercise the exact same set of locales (completeness)", () => {
    const snapshot = loadSnapshot();
    const localeSets = snapshot.exercises.map((exercise) =>
      exercise.translations
        .map((translation) => translation.locale)
        .sort()
        .join(","),
    );

    expect(new Set(localeSets).size).toBe(1);
  });

  it("is sorted by id", () => {
    const snapshot = loadSnapshot();
    const ids = snapshot.exercises.map((exercise) => exercise.id);

    expect(ids).toEqual([...ids].sort((a, b) => a.localeCompare(b)));
  });

  it("has roughly 150 curated exercises", () => {
    const snapshot = loadSnapshot();

    expect(snapshot.exercises.length).toBeGreaterThanOrEqual(100);
    expect(snapshot.exercises.length).toBeLessThanOrEqual(200);
  });

  it("contains no HTML markup or undecoded entity in any instructions text", () => {
    const snapshot = loadSnapshot();
    const tagOrEntityPattern = /<\/?[a-zA-Z]|&[a-zA-Z#][a-zA-Z0-9]*;/;

    for (const exercise of snapshot.exercises) {
      for (const translation of exercise.translations) {
        if (translation.instructions) {
          expect(translation.instructions).not.toMatch(tagOrEntityPattern);
        }
      }
    }
  });
});

describe("curation.json and exercises.json agree", () => {
  it("has the same set of ids in both files", () => {
    const curation = loadCuration();
    const snapshot = loadSnapshot();

    const curationIds = new Set(curation.exercises.map((entry) => entry.id));
    const snapshotIds = new Set(snapshot.exercises.map((exercise) => exercise.id));

    expect(snapshotIds).toEqual(curationIds);
  });

  it("carries every curation name override through to the snapshot", () => {
    const curation = loadCuration();
    const snapshot = loadSnapshot();
    const snapshotById = new Map(snapshot.exercises.map((exercise) => [exercise.id, exercise]));

    for (const entry of curation.exercises) {
      const exercise = snapshotById.get(entry.id);
      expect(exercise).toBeDefined();
      if (!exercise) {
        continue;
      }
      for (const [locale, name] of Object.entries(entry.names)) {
        const translation = exercise.translations.find((t) => t.locale === locale);
        expect(translation?.name).toBe(name);
      }
    }
  });
});

describe("the generated ATTRIBUTION.md", () => {
  it("lists every exercise id's source URL and wger's original title", () => {
    const snapshot = loadSnapshot();
    const attribution = readFileSync(ATTRIBUTION_PATH, "utf8");

    for (const exercise of snapshot.exercises) {
      expect(attribution).toContain(exercise.source.url);
      expect(attribution).toContain(exercise.source.originalName);
    }
  });

  it("states the compilation's own licence", () => {
    const attribution = readFileSync(ATTRIBUTION_PATH, "utf8");

    expect(attribution).toContain("CC BY-SA 4.0");
  });

  it("says modified but not translated, since the committed catalog ships English-only", () => {
    const attribution = readFileSync(ATTRIBUTION_PATH, "utf8");

    expect(attribution).toContain("have been modified by Repz");
    expect(attribution).not.toContain("translated");
  });
});
