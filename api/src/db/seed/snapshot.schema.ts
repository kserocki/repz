import { z } from "zod";
import { INSTRUCTIONS_MAX, LocaleSchema, NAME_MAX } from "../../modules/sync/sync.schemas.ts";
import { EQUIPMENT, MUSCLES } from "../schema/vocabularies.ts";

const SnapshotTranslationSchema = z.strictObject({
  locale: LocaleSchema,
  name: z.string().trim().min(1).max(NAME_MAX),
  instructions: z.string().max(INSTRUCTIONS_MAX).nullable(),
});

export type SnapshotTranslation = z.infer<typeof SnapshotTranslationSchema>;

/**
 * One row of the committed `exercises.json`: the generated, on-disk shape, which keeps full wger
 * provenance (`source`) for `ATTRIBUTION.md` and for spotting upstream drift on a re-import.
 * `originalName` is wger's own EN name, untouched by our Title Case rename — CC BY-SA 3.0 §4(c)
 * requires keeping "the title of the Work if supplied". `translations` always includes `"en"`,
 * sorted by locale.
 */
export const SnapshotExerciseSchema = z
  .strictObject({
    id: z.uuid(),
    primaryMuscle: z.enum(MUSCLES),
    secondaryMuscles: z.array(z.enum(MUSCLES)),
    equipment: z.enum(EQUIPMENT),
    license: z.string(),
    author: z.string(),
    source: z.strictObject({
      wgerId: z.number().int().positive(),
      wgerUuid: z.string(),
      url: z.string(),
      originalName: z.string(),
    }),
    translations: z.array(SnapshotTranslationSchema),
  })
  .superRefine((exercise, ctx) => {
    if (exercise.secondaryMuscles.includes(exercise.primaryMuscle)) {
      ctx.addIssue({
        code: "custom",
        path: ["secondaryMuscles"],
        message: "secondaryMuscles must not include primaryMuscle",
      });
    }
    if (!exercise.translations.some((translation) => translation.locale === "en")) {
      ctx.addIssue({
        code: "custom",
        path: ["translations"],
        message: 'translations must include "en"',
      });
    }
  });

export type SnapshotExercise = z.infer<typeof SnapshotExerciseSchema>;

export const SnapshotFileSchema = z.strictObject({
  source: z.string(),
  exercises: z.array(SnapshotExerciseSchema),
});

export type SnapshotFile = z.infer<typeof SnapshotFileSchema>;

/**
 * The seeder-facing shape, shared by the import pipeline and `catalog-seed.*`: flatter than
 * `SnapshotExercise` because the seeder only ever writes `sourceUrl` to the database, never the
 * wger provenance fields.
 */
export type CatalogExercise = {
  id: string;
  primaryMuscle: SnapshotExercise["primaryMuscle"];
  secondaryMuscles: SnapshotExercise["secondaryMuscles"];
  equipment: SnapshotExercise["equipment"];
  license: string;
  author: string;
  sourceUrl: string;
  translations: SnapshotTranslation[];
};

export function toCatalogExercise(exercise: SnapshotExercise): CatalogExercise {
  const { source, ...rest } = exercise;
  return { ...rest, sourceUrl: source.url };
}
