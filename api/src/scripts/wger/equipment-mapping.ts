import type { Equipment } from "../../db/schema/vocabularies.ts";

/**
 * wger's "main" equipment items, mapped to Repz's `EQUIPMENT` vocabulary (verified against the
 * live `/api/v2/equipment/` list). SZ-Bar joins Barbell; a pull-up bar and "none" (bodyweight
 * exercise) both mean `bodyweight`.
 */
export const WGER_EQUIPMENT_TO_MAIN: Readonly<Record<number, Equipment>> = {
  1: "barbell", // Barbell
  2: "barbell", // SZ-Bar
  3: "dumbbell", // Dumbbell
  10: "kettlebell", // Kettlebell
  12: "cable", // Cable machine
  11: "band", // Resistance band
  6: "bodyweight", // Pull-up bar
  7: "bodyweight", // none (bodyweight exercise)
};

/** Bench, Incline bench, Gym mat: ignored next to a main item; alone (no main item) → bodyweight. */
const BENCH_OR_MAT_IDS = new Set([8, 9, 4]);
/** Swiss Ball: ignored next to a main item; alone → other. */
const SWISS_BALL_ID = 5;

/** barbell > dumbbell > kettlebell > cable > band > bodyweight, when several main items are present. */
const MAIN_PRECEDENCE: readonly Equipment[] = [
  "barbell",
  "dumbbell",
  "kettlebell",
  "cable",
  "band",
  "bodyweight",
];

export type EquipmentDerivation = { ok: true; equipment: Equipment } | { ok: false };

/**
 * Derives one `EQUIPMENT` value from wger's equipment list. Empty, or every id being
 * unrecognised, is `ok: false`: the curator sets equipment by hand (usually `machine`, which
 * wger has no equivalent for).
 */
export function deriveEquipment(equipmentIds: readonly number[]): EquipmentDerivation {
  const mains = new Set<Equipment>();
  let hasBenchOrMat = false;
  let hasSwissBall = false;

  for (const id of equipmentIds) {
    const main = WGER_EQUIPMENT_TO_MAIN[id];
    if (main) {
      mains.add(main);
    } else if (BENCH_OR_MAT_IDS.has(id)) {
      hasBenchOrMat = true;
    } else if (id === SWISS_BALL_ID) {
      hasSwissBall = true;
    }
    // An id in none of the sets above (unrecognised) contributes nothing; it never silently
    // becomes a main item, so the exercise still falls through to `ok: false` unless something
    // else present resolves it.
  }

  if (mains.size > 0) {
    const equipment = MAIN_PRECEDENCE.find((candidate) => mains.has(candidate));
    if (equipment) {
      return { ok: true, equipment };
    }
  }
  if (hasBenchOrMat) {
    return { ok: true, equipment: "bodyweight" };
  }
  if (hasSwissBall) {
    return { ok: true, equipment: "other" };
  }
  return { ok: false };
}
