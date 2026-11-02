import type { Muscle } from "../../db/schema/vocabularies.ts";

/**
 * wger's 15 muscles mapped down to Repz's 12 groups (Q2, approved 2026-11-02, verified against
 * the live `/api/v2/muscle/` list). Exhaustive over every id wger has today: Serratus anterior →
 * chest, Trapezius → back, Brachialis → biceps; every other wger muscle maps 1:1. wger has no
 * forearms, full_body or machine; those are only ever set by a curation override.
 */
export const WGER_MUSCLE_TO_GROUP: Readonly<Record<number, Muscle>> = {
  4: "chest", // Pectoralis major
  3: "chest", // Serratus anterior
  12: "back", // Latissimus dorsi
  9: "back", // Trapezius
  2: "shoulders", // Anterior deltoid
  1: "biceps", // Biceps brachii
  13: "biceps", // Brachialis
  5: "triceps", // Triceps brachii
  6: "abs", // Rectus abdominis
  14: "abs", // Obliquus externus abdominis
  8: "glutes", // Gluteus maximus
  10: "quadriceps", // Quadriceps femoris
  11: "hamstrings", // Biceps femoris
  7: "calves", // Gastrocnemius
  15: "calves", // Soleus
};

export type MuscleGroupDerivation =
  | { ok: true; primaryGroups: Muscle[]; secondaryGroups: Muscle[] }
  | { ok: false; unknownMuscleId: number };

/**
 * Maps wger's primary/secondary muscle ids down to Repz's 12 groups, de-duplicating within each
 * list but not yet deciding a single primary (that needs the curation override, done by the
 * caller — `build-snapshot.ts`). An id outside `WGER_MUSCLE_TO_GROUP` fails the whole derivation:
 * a new upstream muscle must never map silently.
 */
export function deriveMuscleGroups(
  primaryIds: readonly number[],
  secondaryIds: readonly number[],
): MuscleGroupDerivation {
  function mapIds(ids: readonly number[]): Muscle[] | { unknownMuscleId: number } {
    const groups: Muscle[] = [];
    for (const id of ids) {
      const group = WGER_MUSCLE_TO_GROUP[id];
      if (!group) {
        return { unknownMuscleId: id };
      }
      if (!groups.includes(group)) {
        groups.push(group);
      }
    }
    return groups;
  }

  const primaryGroups = mapIds(primaryIds);
  if (!Array.isArray(primaryGroups)) {
    return { ok: false, unknownMuscleId: primaryGroups.unknownMuscleId };
  }
  const secondaryGroups = mapIds(secondaryIds);
  if (!Array.isArray(secondaryGroups)) {
    return { ok: false, unknownMuscleId: secondaryGroups.unknownMuscleId };
  }

  return { ok: true, primaryGroups, secondaryGroups };
}
