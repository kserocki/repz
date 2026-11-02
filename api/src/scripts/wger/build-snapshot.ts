import { type Equipment, MUSCLES, type Muscle } from "../../db/schema/vocabularies.ts";
import type { CurationFile } from "../../db/seed/curation.schema.ts";
import type {
  SnapshotExercise,
  SnapshotFile,
  SnapshotTranslation,
} from "../../db/seed/snapshot.schema.ts";
import { INSTRUCTIONS_MAX, NAME_MAX } from "../../modules/sync/sync.schemas.ts";
import { collectAuthors, resolveLicense, sourceUrl } from "./attribution.ts";
import { deriveEquipment } from "./equipment-mapping.ts";
import { htmlToPlainText } from "./instructions-text.ts";
import { deriveMuscleGroups } from "./muscle-mapping.ts";
import { findTranslation, WGER_LANGUAGE_EN, type WgerExerciseInfo } from "./wger.schemas.ts";

export const WGER_SOURCE_URL = "https://wger.de/api/v2/exerciseinfo/";

export type BuildSnapshotResult =
  | { ok: true; snapshot: SnapshotFile }
  | { ok: false; problems: string[] };

/**
 * A curation entry's locales besides `"en"`, sorted. Used to enforce the completeness rule:
 * every entry must offer the exact same set of non-English locales (language-agnostic — no
 * hard-coded `"pl"`), so a language is never half-added to the catalog.
 */
function extraLocalesOf(entry: CurationFile["exercises"][number]): string[] {
  return Object.keys(entry.names)
    .filter((locale) => locale !== "en")
    .sort();
}

function checkLocaleCompleteness(curation: CurationFile): string[] {
  if (curation.exercises.length === 0) {
    return [];
  }
  const expected = extraLocalesOf(curation.exercises[0] as CurationFile["exercises"][number]);
  const expectedKey = expected.join(",");
  const problems: string[] = [];
  for (const entry of curation.exercises) {
    const locales = extraLocalesOf(entry);
    if (locales.join(",") !== expectedKey) {
      problems.push(
        `curation ${entry.id}: locales [${locales.join(", ") || "none"}] do not match the ` +
          `catalog's locales [${expected.join(", ") || "none"}] (every entry must offer the same locales)`,
      );
    }
  }
  return problems;
}

function buildTranslations(
  entry: CurationFile["exercises"][number],
  enName: string,
  enInstructions: string | null,
): SnapshotTranslation[] {
  const translations: SnapshotTranslation[] = [
    { locale: "en", name: enName, instructions: enInstructions },
  ];
  for (const locale of Object.keys(entry.names).filter((candidate) => candidate !== "en")) {
    translations.push({
      locale,
      name: entry.names[locale] as string,
      instructions: entry.instructions?.[locale] ?? null,
    });
  }
  return translations.sort((a, b) => a.locale.localeCompare(b.locale));
}

/**
 * Derives one entry's primary/secondary muscles: the curation override wins outright; otherwise
 * wger's mapped primary muscles must collapse to exactly one group, or it is a problem (0 or 2+
 * groups means the curator must set `primaryMuscle` by hand — Q2).
 */
function deriveMusclesForEntry(
  entry: CurationFile["exercises"][number],
  wger: WgerExerciseInfo,
  problems: string[],
): { primaryMuscle: Muscle; secondaryMuscles: Muscle[] } | null {
  const derivation = deriveMuscleGroups(
    wger.muscles.map((muscle) => muscle.id),
    wger.muscles_secondary.map((muscle) => muscle.id),
  );
  if (!derivation.ok) {
    problems.push(
      `curation ${entry.id} (wgerId ${entry.wgerId}): unknown wger muscle id ${derivation.unknownMuscleId}`,
    );
    return null;
  }

  let primaryMuscle: Muscle;
  if (entry.primaryMuscle) {
    primaryMuscle = entry.primaryMuscle;
  } else if (derivation.primaryGroups.length === 1) {
    primaryMuscle = derivation.primaryGroups[0] as Muscle;
  } else {
    const groups = derivation.primaryGroups.join(", ") || "no group";
    problems.push(
      `curation ${entry.id} (wgerId ${entry.wgerId}): ambiguous primary muscle (wger's primaries map to ${groups}); set primaryMuscle in curation`,
    );
    return null;
  }

  let secondaryMuscles: Muscle[];
  if (entry.secondaryMuscles) {
    secondaryMuscles = entry.secondaryMuscles;
  } else {
    const groups = new Set<Muscle>([...derivation.primaryGroups, ...derivation.secondaryGroups]);
    groups.delete(primaryMuscle);
    secondaryMuscles = MUSCLES.filter((muscle) => groups.has(muscle));
  }

  return { primaryMuscle, secondaryMuscles };
}

function deriveEquipmentForEntry(
  entry: CurationFile["exercises"][number],
  wger: WgerExerciseInfo,
  problems: string[],
): Equipment | null {
  if (entry.equipment) {
    return entry.equipment;
  }
  const derivation = deriveEquipment(wger.equipment.map((item) => item.id));
  if (!derivation.ok) {
    problems.push(
      `curation ${entry.id} (wgerId ${entry.wgerId}): equipment could not be derived (empty or unrecognised); set equipment in curation`,
    );
    return null;
  }
  return derivation.equipment;
}

/**
 * Pure: `(wger data, curation)` → the committed snapshot, or every problem found (the importer
 * writes nothing and exits 1 on any problem). Order-independent in the wger data (a re-import
 * with reshuffled pages produces byte-identical output); the output is sorted by `id`.
 */
export function buildSnapshot(
  wgerExercises: readonly WgerExerciseInfo[],
  curation: CurationFile,
): BuildSnapshotResult {
  const wgerById = new Map(wgerExercises.map((exercise) => [exercise.id, exercise]));
  const problems: string[] = [...checkLocaleCompleteness(curation)];
  const built: SnapshotExercise[] = [];

  for (const entry of curation.exercises) {
    const wger = wgerById.get(entry.wgerId);
    if (!wger) {
      problems.push(`curation ${entry.id}: wger exercise ${entry.wgerId} not found`);
      continue;
    }

    const enTranslation = findTranslation(wger, WGER_LANGUAGE_EN);
    if (!enTranslation) {
      problems.push(
        `curation ${entry.id} (wgerId ${entry.wgerId}): wger exercise has no EN translation`,
      );
      continue;
    }

    const muscles = deriveMusclesForEntry(entry, wger, problems);
    const equipment = deriveEquipmentForEntry(entry, wger, problems);

    const licenseResolution = resolveLicense(wger.license.short_name, enTranslation.license);
    if (!licenseResolution.ok) {
      problems.push(
        `curation ${entry.id} (wgerId ${entry.wgerId}): unknown or unsupported licence`,
      );
    }

    const enName = entry.names.en ?? enTranslation.name;
    if (enName.length > NAME_MAX) {
      problems.push(
        `curation ${entry.id} (wgerId ${entry.wgerId}): EN name is over ${NAME_MAX} characters`,
      );
    }

    let enInstructions: string | null | undefined = entry.instructions?.en;
    if (enInstructions === undefined) {
      const htmlResult = htmlToPlainText(enTranslation.description);
      if (!htmlResult.ok) {
        problems.push(
          `curation ${entry.id} (wgerId ${entry.wgerId}): unknown HTML tag <${htmlResult.unknownTag}> in the EN description`,
        );
        continue;
      }
      enInstructions = htmlResult.text;
      if (enInstructions && enInstructions.length > INSTRUCTIONS_MAX) {
        problems.push(
          `curation ${entry.id} (wgerId ${entry.wgerId}): EN instructions are over ${INSTRUCTIONS_MAX} characters`,
        );
        continue;
      }
    }

    if (!muscles || !equipment || !licenseResolution.ok) {
      continue;
    }

    built.push({
      id: entry.id,
      primaryMuscle: muscles.primaryMuscle,
      secondaryMuscles: muscles.secondaryMuscles,
      equipment,
      license: licenseResolution.license,
      author: collectAuthors(
        wger.license_author,
        enTranslation.license_author,
        enTranslation.author_history,
      ),
      source: { wgerId: entry.wgerId, wgerUuid: wger.uuid, url: sourceUrl(entry.wgerId) },
      translations: buildTranslations(entry, enName, enInstructions),
    });
  }

  if (problems.length > 0) {
    return { ok: false, problems };
  }

  const sorted = [...built].sort((a, b) => a.id.localeCompare(b.id));
  return { ok: true, snapshot: { source: WGER_SOURCE_URL, exercises: sorted } };
}
