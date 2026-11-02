import { z } from "zod";

/**
 * The subset of wger's `GET /exerciseinfo/` response we read (verified against the live API,
 * 2026-11-02). Plain `z.object` (not `.strict()`): wger may add fields freely, but if it renames,
 * removes or retypes one of the fields below, parsing fails loudly instead of silently importing
 * garbage. `license` here is wger's small, effectively closed license vocabulary (`attribution.ts`
 * maps the 5 known ids); an id outside it is a problem the importer reports, never a crash.
 */
const WgerMuscleRefSchema = z.object({ id: z.number().int() });
const WgerEquipmentRefSchema = z.object({ id: z.number().int() });

const WgerTranslationSchema = z.object({
  language: z.number().int(),
  name: z.string(),
  /** HTML, `<p>`/`<li>`/`<ol>`/`<ul>`/`<strong>`/`<em>` only (verified live); `instructions-text.ts` converts it. */
  description: z.string(),
  /** wger's license id (1–5 today); not an object on the translation, unlike the exercise-level `license`. */
  license: z.number().int(),
  license_author: z.string().nullable(),
  author_history: z.array(z.string()),
});

export const WgerExerciseInfoSchema = z.object({
  id: z.number().int(),
  uuid: z.string(),
  muscles: z.array(WgerMuscleRefSchema),
  muscles_secondary: z.array(WgerMuscleRefSchema),
  equipment: z.array(WgerEquipmentRefSchema),
  license: z.object({ short_name: z.string() }),
  license_author: z.string().nullable(),
  translations: z.array(WgerTranslationSchema),
});

export const WgerExerciseInfoPageSchema = z.object({
  count: z.number().int(),
  next: z.string().nullable(),
  results: z.array(WgerExerciseInfoSchema),
});

export type WgerExerciseInfo = z.infer<typeof WgerExerciseInfoSchema>;
export type WgerTranslation = z.infer<typeof WgerTranslationSchema>;
export type WgerExerciseInfoPage = z.infer<typeof WgerExerciseInfoPageSchema>;

/** wger's language ids for the two locales REP-16 cares about (verified live, 2026-11-02). */
export const WGER_LANGUAGE_EN = 2;
export const WGER_LANGUAGE_PL = 14;

export function findTranslation(
  exercise: WgerExerciseInfo,
  language: number,
): WgerTranslation | undefined {
  return exercise.translations.find((translation) => translation.language === language);
}
