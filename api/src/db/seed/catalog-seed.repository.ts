import { and, asc, eq, inArray, isNotNull, isNull, sql } from "drizzle-orm";
import type { PostgresJsDatabase } from "drizzle-orm/postgres-js";
import { lockCatalogSyncWrites } from "../../modules/sync/sync.repository.ts";
import type { DatabaseExecutor, DatabaseSchema } from "../client.ts";
import { exercises, exerciseTranslations } from "../schema/index.ts";
import { nextSyncVersion } from "../schema/sync.ts";
import type { Equipment, Muscle } from "../schema/vocabularies.ts";
import type { StoredCatalogExercise } from "./catalog-seed.plan.ts";
import type { CatalogSeedStore, CatalogSeedTransactions } from "./catalog-seed.service.ts";
import type { SnapshotTranslation } from "./snapshot.schema.ts";

function enNameOf(translations: readonly SnapshotTranslation[]): string {
  const en = translations.find((translation) => translation.locale === "en");
  if (!en) {
    // snapshot.schema.ts's CatalogExerciseSchema already requires an "en" translation; this is a
    // safety net against a future caller bypassing that schema.
    throw new Error('A catalog exercise has no "en" translation');
  }
  return en.name;
}

async function insertTranslations(
  executor: DatabaseExecutor,
  exerciseId: string,
  translations: readonly SnapshotTranslation[],
): Promise<void> {
  if (translations.length === 0) {
    return;
  }
  await executor
    .insert(exerciseTranslations)
    .values(translations.map((translation) => ({ exerciseId, ...translation })));
}

/** Drizzle-only implementation of `CatalogSeedStore` (ADR-0021). */
export function createCatalogSeedStore(executor: DatabaseExecutor): CatalogSeedStore {
  return {
    async lockCatalogSyncWrites() {
      await lockCatalogSyncWrites(executor);
    },

    async loadCatalog(): Promise<StoredCatalogExercise[]> {
      const rows = await executor.select().from(exercises).where(isNull(exercises.ownerId));
      const ids = rows.map((row) => row.id);
      const translationRows =
        ids.length === 0
          ? []
          : await executor
              .select()
              .from(exerciseTranslations)
              .where(inArray(exerciseTranslations.exerciseId, ids))
              .orderBy(asc(exerciseTranslations.exerciseId), asc(exerciseTranslations.locale));

      const translationsByExercise = new Map<string, SnapshotTranslation[]>();
      for (const row of translationRows) {
        const list = translationsByExercise.get(row.exerciseId) ?? [];
        list.push({ locale: row.locale, name: row.name, instructions: row.instructions });
        translationsByExercise.set(row.exerciseId, list);
      }

      return rows.map((row) => ({
        id: row.id,
        primaryMuscle: row.primaryMuscle as Muscle,
        secondaryMuscles: row.secondaryMuscles as Muscle[],
        equipment: row.equipment as Equipment,
        license: row.license ?? "",
        author: row.author ?? "",
        sourceUrl: row.sourceUrl ?? "",
        translations: translationsByExercise.get(row.id) ?? [],
        deleted: row.deletedAt !== null,
      }));
    },

    async findCustomIds(ids) {
      if (ids.length === 0) {
        return [];
      }
      const rows = await executor
        .select({ id: exercises.id })
        .from(exercises)
        .where(and(inArray(exercises.id, ids), isNotNull(exercises.ownerId)));
      return rows.map((row) => row.id);
    },

    async insert(row) {
      await executor.insert(exercises).values({
        id: row.id,
        ownerId: null,
        name: enNameOf(row.translations),
        primaryMuscle: row.primaryMuscle,
        secondaryMuscles: row.secondaryMuscles,
        equipment: row.equipment,
        license: row.license,
        author: row.author,
        sourceUrl: row.sourceUrl,
      });
      await insertTranslations(executor, row.id, row.translations);
    },

    async update(row) {
      await executor
        .update(exercises)
        .set({
          name: enNameOf(row.translations),
          primaryMuscle: row.primaryMuscle,
          secondaryMuscles: row.secondaryMuscles,
          equipment: row.equipment,
          license: row.license,
          author: row.author,
          sourceUrl: row.sourceUrl,
          version: nextSyncVersion,
          updatedAt: sql`now()`,
        })
        .where(eq(exercises.id, row.id));
      await executor
        .delete(exerciseTranslations)
        .where(eq(exerciseTranslations.exerciseId, row.id));
      await insertTranslations(executor, row.id, row.translations);
    },

    async tombstone(id) {
      await executor
        .update(exercises)
        .set({ deletedAt: sql`now()`, version: nextSyncVersion, updatedAt: sql`now()` })
        .where(eq(exercises.id, id));
      await executor.delete(exerciseTranslations).where(eq(exerciseTranslations.exerciseId, id));
    },
  };
}

/** `write` is `READ COMMITTED` (Postgres's default): one transaction for the whole seed run. */
export function createCatalogSeedTransactions(
  db: PostgresJsDatabase<DatabaseSchema>,
): CatalogSeedTransactions {
  return {
    write(work) {
      return db.transaction((tx) => work(createCatalogSeedStore(tx)), {
        isolationLevel: "read committed",
      });
    },
  };
}
