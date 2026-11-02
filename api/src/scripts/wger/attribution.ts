/**
 * wger's license vocabulary (verified live against `/api/v2/license/`, 2026-11-02): 5 ids total,
 * a closed list. The exercise-level `license` field is the object `{ short_name, ... }`; the
 * per-translation `license` field is just this numeric id, so resolving a translation's license
 * needs this table.
 */
const WGER_LICENSE_SHORT_NAME_BY_ID: Readonly<Record<number, string>> = {
  1: "CC-BY-SA 3",
  2: "CC-BY-SA 4",
  3: "CC0",
  4: "CC-BY 4",
  5: "ODbL",
};

type LicenseFamily = "CC-BY-SA" | "CC-BY" | "CC0";

/** CC-BY-SA is the most restrictive of the three families we accept, then CC-BY, then CC0. */
const FAMILY_RESTRICTIVENESS: Readonly<Record<LicenseFamily, number>> = {
  "CC-BY-SA": 2,
  "CC-BY": 1,
  CC0: 0,
};

/** `"CC-BY-SA 3"` → `"CC-BY-SA 3.0"`, `"CC0"` → `"CC0 1.0"`. `null` for ODbL or an unknown name. */
export function normaliseLicense(shortName: string): string | null {
  switch (shortName) {
    case "CC-BY-SA 3":
      return "CC-BY-SA 3.0";
    case "CC-BY-SA 4":
      return "CC-BY-SA 4.0";
    case "CC-BY 4":
      return "CC-BY 4.0";
    case "CC0":
      return "CC0 1.0";
    default:
      return null;
  }
}

function familyOf(normalised: string): LicenseFamily | null {
  if (normalised.startsWith("CC-BY-SA")) {
    return "CC-BY-SA";
  }
  if (normalised.startsWith("CC-BY")) {
    return "CC-BY";
  }
  if (normalised.startsWith("CC0")) {
    return "CC0";
  }
  return null;
}

/**
 * The more restrictive of two already-normalised licenses (CC-BY-SA > CC-BY > CC0). The spec
 * only orders families, not versions within one: when both are the same family (e.g.
 * CC-BY-SA 3.0 vs. 4.0 — wger's exercise- and translation-level licenses occasionally differ
 * this way), `a` (the exercise's own license) wins, since CC §4(b) ties the Work's license to
 * the exercise record, and the translation only ever narrows it to a stricter family.
 */
export function mostRestrictiveLicense(a: string, b: string): string {
  const familyA = familyOf(a);
  const familyB = familyOf(b);
  if (familyA === null) {
    return b;
  }
  if (familyB === null) {
    return a;
  }
  return FAMILY_RESTRICTIVENESS[familyA] >= FAMILY_RESTRICTIVENESS[familyB] ? a : b;
}

export type LicenseResolution = { ok: true; license: string } | { ok: false };

/**
 * The most restrictive of the exercise's own license and its EN translation's license (both
 * normalised first). `ok: false` for ODbL or an id wger hasn't published (ADR-0021): the curator
 * picks a different source exercise for the same movement rather than storing an unclear license.
 */
export function resolveLicense(
  exerciseLicenseShortName: string,
  translationLicenseId: number,
): LicenseResolution {
  const exerciseNormalised = normaliseLicense(exerciseLicenseShortName);
  const translationShortName = WGER_LICENSE_SHORT_NAME_BY_ID[translationLicenseId];
  const translationNormalised = translationShortName
    ? normaliseLicense(translationShortName)
    : null;

  if (!exerciseNormalised || !translationNormalised) {
    return { ok: false };
  }
  return { ok: true, license: mostRestrictiveLicense(exerciseNormalised, translationNormalised) };
}

/**
 * Combines the exercise's own `license_author`, the EN translation's `license_author` and its
 * `author_history`, dropping empty values and duplicates and joining the rest with `", "`.
 * `"wger.de"` when nothing is left (ADR-0021; Q5c covers the rare e-mail-as-author case, handled
 * during curation by picking a different source exercise, never here).
 */
export function collectAuthors(
  exerciseLicenseAuthor: string | null,
  translationLicenseAuthor: string | null,
  translationAuthorHistory: readonly string[],
): string {
  const candidates = [exerciseLicenseAuthor, translationLicenseAuthor, ...translationAuthorHistory];
  const seen = new Set<string>();
  const authors: string[] = [];
  for (const candidate of candidates) {
    const trimmed = candidate?.trim();
    if (trimmed && !seen.has(trimmed)) {
      seen.add(trimmed);
      authors.push(trimmed);
    }
  }
  return authors.length > 0 ? authors.join(", ") : "wger.de";
}

/** The public wger page for an exercise, used as `sourceUrl` and the ATTRIBUTION.md link. */
export function sourceUrl(wgerId: number): string {
  return `https://wger.de/exercise/${wgerId}/view-base`;
}

/** The CC legal-code link for a normalised license string, for ATTRIBUTION.md. */
export function licenseUrl(normalisedLicense: string): string {
  switch (normalisedLicense) {
    case "CC-BY-SA 3.0":
      return "https://creativecommons.org/licenses/by-sa/3.0/";
    case "CC-BY-SA 4.0":
      return "https://creativecommons.org/licenses/by-sa/4.0/";
    case "CC-BY 4.0":
      return "https://creativecommons.org/licenses/by/4.0/";
    case "CC0 1.0":
      return "https://creativecommons.org/publicdomain/zero/1.0/";
    default:
      return "https://creativecommons.org/";
  }
}
