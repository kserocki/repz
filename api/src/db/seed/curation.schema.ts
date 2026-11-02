import { z } from "zod";
import { INSTRUCTIONS_MAX, LOCALE_PATTERN, NAME_MAX } from "../../modules/sync/sync.schemas.ts";
import { EQUIPMENT, MUSCLES } from "../schema/vocabularies.ts";

const nameField = z.string().trim().min(1).max(NAME_MAX);

/**
 * `{ en?: string, ... }`: every key is optional and locale-keyed, validated only against the
 * BCP 47 pattern — no locale is required or special-cased. `en` overrides wger's own EN name when
 * present; the catalog ships English-only today, but adding a language later is adding more keys
 * here (plus a re-import and re-seed), never a schema change.
 */
const CurationNamesSchema = z.record(z.string(), nameField).superRefine((names, ctx) => {
  for (const locale of Object.keys(names)) {
    if (!LOCALE_PATTERN.test(locale)) {
      ctx.addIssue({
        code: "custom",
        path: [locale],
        message: `"${locale}" is not a valid locale`,
      });
    }
  }
});

/** Optional per-locale overrides of the derived instructions text (e.g. rewriting a poor EN description). */
const CurationInstructionsSchema = z
  .record(z.string(), z.string().trim().min(1).max(INSTRUCTIONS_MAX))
  .superRefine((instructions, ctx) => {
    for (const locale of Object.keys(instructions)) {
      if (!LOCALE_PATTERN.test(locale)) {
        ctx.addIssue({
          code: "custom",
          path: [locale],
          message: `"${locale}" is not a valid locale`,
        });
      }
    }
  });

export const CurationEntrySchema = z
  .strictObject({
    id: z.uuid(),
    wgerId: z.number().int().positive(),
    names: CurationNamesSchema,
    instructions: CurationInstructionsSchema.optional(),
    primaryMuscle: z.enum(MUSCLES).optional(),
    secondaryMuscles: z.array(z.enum(MUSCLES)).optional(),
    equipment: z.enum(EQUIPMENT).optional(),
    reviewNote: z.string().optional(),
  })
  .superRefine((entry, ctx) => {
    if (entry.primaryMuscle && entry.secondaryMuscles?.includes(entry.primaryMuscle)) {
      ctx.addIssue({
        code: "custom",
        path: ["secondaryMuscles"],
        message: "secondaryMuscles must not include primaryMuscle",
      });
    }
    if (
      entry.secondaryMuscles &&
      new Set(entry.secondaryMuscles).size !== entry.secondaryMuscles.length
    ) {
      ctx.addIssue({
        code: "custom",
        path: ["secondaryMuscles"],
        message: "secondaryMuscles must not contain duplicates",
      });
    }
    // The routine EN Title Case rename (`names.en`) needs no justification, but a muscle or
    // equipment override always changes what the data says and must carry a reason (ADR-0021).
    const hasDataOverride =
      entry.primaryMuscle !== undefined ||
      entry.secondaryMuscles !== undefined ||
      entry.equipment !== undefined;
    if (hasDataOverride && !entry.reviewNote?.trim()) {
      ctx.addIssue({
        code: "custom",
        path: ["reviewNote"],
        message:
          "reviewNote is required whenever primaryMuscle, secondaryMuscles or equipment is overridden",
      });
    }
  });

export type CurationEntry = z.infer<typeof CurationEntrySchema>;

export const CurationFileSchema = z
  .strictObject({ exercises: z.array(CurationEntrySchema) })
  .superRefine((file, ctx) => {
    const seenIds = new Set<string>();
    const seenWgerIds = new Set<number>();
    file.exercises.forEach((entry, index) => {
      if (seenIds.has(entry.id)) {
        ctx.addIssue({
          code: "custom",
          path: ["exercises", index, "id"],
          message: `Duplicate curation id: ${entry.id}`,
        });
      }
      seenIds.add(entry.id);

      if (seenWgerIds.has(entry.wgerId)) {
        ctx.addIssue({
          code: "custom",
          path: ["exercises", index, "wgerId"],
          message: `Duplicate wgerId: ${entry.wgerId}`,
        });
      }
      seenWgerIds.add(entry.wgerId);
    });
  });

export type CurationFile = z.infer<typeof CurationFileSchema>;
