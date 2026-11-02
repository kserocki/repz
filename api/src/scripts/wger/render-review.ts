import { MUSCLES } from "../../db/schema/vocabularies.ts";
import type { CurationEntry, CurationFile } from "../../db/seed/curation.schema.ts";
import type { SnapshotExercise, SnapshotFile } from "../../db/seed/snapshot.schema.ts";

function nameOf(exercise: SnapshotExercise, locale: string): string {
  return exercise.translations.find((translation) => translation.locale === locale)?.name ?? "—";
}

/** Every locale besides `en` present anywhere in the snapshot, sorted. `[]` while the catalog ships English-only. */
function extraLocalesIn(snapshot: SnapshotFile): string[] {
  const locales = new Set<string>();
  for (const exercise of snapshot.exercises) {
    for (const translation of exercise.translations) {
      if (translation.locale !== "en") {
        locales.add(translation.locale);
      }
    }
  }
  return [...locales].sort();
}

function overridesOf(entry: CurationEntry | undefined): string {
  if (!entry) {
    return "—";
  }
  const overrides: string[] = [];
  if (entry.names.en) {
    overrides.push("EN name");
  }
  if (entry.instructions?.en !== undefined) {
    overrides.push("EN instructions rewritten");
  }
  if (entry.primaryMuscle) {
    overrides.push("primary muscle");
  }
  if (entry.secondaryMuscles) {
    overrides.push("secondary muscles");
  }
  if (entry.equipment) {
    overrides.push("equipment");
  }
  if (overrides.length === 0) {
    return "—";
  }
  return entry.reviewNote ? `${overrides.join(", ")} (${entry.reviewNote})` : overrides.join(", ");
}

/**
 * Whether `entry` overrides a *derived* field (muscle or equipment) rather than just the routine
 * EN Title Case rename every entry gets (Q3) — the summary line counts these separately, since a
 * 100% "has an override" count would say nothing useful about how much the wger data needed
 * correcting.
 */
function hasDataOverride(entry: CurationEntry): boolean {
  return (
    entry.primaryMuscle !== undefined ||
    entry.secondaryMuscles !== undefined ||
    entry.equipment !== undefined
  );
}

function countBy<T extends string>(values: readonly T[]): Map<T, number> {
  const counts = new Map<T, number>();
  for (const value of values) {
    counts.set(value, (counts.get(value) ?? 0) + 1);
  }
  return counts;
}

function renderCounts<T extends string>(counts: Map<T, number>): string {
  return [...counts.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([key, count]) => `${key} ${count}`)
    .join(", ");
}

/**
 * The markdown table for the WP-4 user gate: entries grouped by primary muscle, with every
 * manual override shown for cross-checking. No locale is special-cased: the catalog ships
 * English-only today, so every row shows just the EN name; if a later import adds another
 * locale's names, this table grows one "<locale> name" column per locale automatically, with no
 * code change. Never read by any other code; purely for the person approving the catalog.
 */
export function renderReviewTable(snapshot: SnapshotFile, curation: CurationFile): string {
  const curationById = new Map(curation.exercises.map((entry) => [entry.id, entry]));
  const extraLocales = extraLocalesIn(snapshot);

  const total = snapshot.exercises.length;
  const groupCounts = countBy(snapshot.exercises.map((exercise) => exercise.primaryMuscle));
  const equipmentCounts = countBy(snapshot.exercises.map((exercise) => exercise.equipment));
  const licenceCounts = countBy(snapshot.exercises.map((exercise) => exercise.license));
  const dataOverrideCount = curation.exercises.filter(hasDataOverride).length;
  const rewrittenCount = curation.exercises.filter(
    (entry) => entry.instructions?.en !== undefined,
  ).length;

  const summary = [
    `# Exercise catalog review`,
    "",
    `**${total} exercises.** Per primary group: ${renderCounts(groupCounts)}.`,
    `Per equipment: ${renderCounts(equipmentCounts)}.`,
    `Per licence: ${renderCounts(licenceCounts)}.`,
    `Every EN name was Title-Cased from wger's original. ${dataOverrideCount} entries override a ` +
      `derived muscle or equipment value (see the Overrides column for the reason). ` +
      `${rewrittenCount} EN instructions were rewritten.`,
    "",
  ];

  const localeHeaders = extraLocales.map((locale) => `${locale} name`);
  const headerCells = [
    "EN name",
    ...localeHeaders,
    "Primary",
    "Secondary",
    "Equipment",
    "Licence",
    "Author",
    "wger",
    "Overrides",
  ];
  const headerRow = `| ${headerCells.join(" | ")} |`;
  const separatorRow = `|${headerCells.map(() => "---").join("|")}|`;

  const sections = MUSCLES.filter((muscle) => groupCounts.has(muscle)).flatMap((muscle) => {
    const exercises = snapshot.exercises.filter((exercise) => exercise.primaryMuscle === muscle);
    const rows = exercises.map((exercise) => {
      const entry = curationById.get(exercise.id);
      const wgerLink = `[${entry?.wgerId}](${exercise.source.url})`;
      const cells = [
        nameOf(exercise, "en"),
        ...extraLocales.map((locale) => nameOf(exercise, locale)),
        exercise.primaryMuscle,
        exercise.secondaryMuscles.join(", ") || "—",
        exercise.equipment,
        exercise.license,
        exercise.author,
        wgerLink,
        overridesOf(entry),
      ];
      return `| ${cells.join(" | ")} |`;
    });

    return [`## ${muscle} (${exercises.length})`, "", headerRow, separatorRow, ...rows, ""];
  });

  return [...summary, ...sections].join("\n");
}
