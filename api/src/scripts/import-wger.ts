import { readFile, writeFile } from "node:fs/promises";
import { z } from "zod";
import { CurationFileSchema } from "../db/seed/curation.schema.ts";
import { SnapshotFileSchema } from "../db/seed/snapshot.schema.ts";
import { loadEnv } from "../env.ts";
import { createLogger } from "../shared/logger.ts";
import { buildSnapshot } from "./wger/build-snapshot.ts";
import { renderAttributionMarkdown } from "./wger/render-attribution.ts";
import { renderReviewTable } from "./wger/render-review.ts";
import { type FetchLike, fetchAllExerciseInfo } from "./wger/wger-client.ts";

/**
 * `pnpm import:wger`: fetches the live wger catalog (network, run by hand, never in CI), curates
 * it against the committed `curation.json`, and writes the committed `exercises.json` +
 * `ATTRIBUTION.md`. Any problem (§3.7) writes nothing and exits 1, listing every problem found.
 */
const WGER_BASE_URL = "https://wger.de/api/v2";
const SEED_DIR = new URL("../db/seed/", import.meta.url);
const CURATION_PATH = new URL("curation.json", SEED_DIR);
const SNAPSHOT_PATH = new URL("exercises.json", SEED_DIR);
const ATTRIBUTION_PATH = new URL("ATTRIBUTION.md", SEED_DIR);

const scriptEnvSchema = z.object({
  LOG_LEVEL: z.enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"]).default("info"),
});
const env = loadEnv(process.env, scriptEnvSchema);
const logger = createLogger(env);

function parseArgs(argv: readonly string[]): { reviewPath: string | null } {
  const index = argv.indexOf("--review");
  if (index === -1) {
    return { reviewPath: null };
  }
  const value = argv[index + 1];
  if (!value) {
    throw new Error("--review requires a path argument");
  }
  return { reviewPath: value };
}

async function main(): Promise<number> {
  const { reviewPath } = parseArgs(process.argv.slice(2));
  const fetchImpl: FetchLike = (input, init) => fetch(input, init);

  logger.info({ baseUrl: WGER_BASE_URL }, "fetching wger exercise data");
  const wgerExercises = await fetchAllExerciseInfo(fetchImpl, WGER_BASE_URL);
  logger.info({ count: wgerExercises.length }, "fetched wger exercises");

  const curationRaw: unknown = JSON.parse(await readFile(CURATION_PATH, "utf8"));
  const curationResult = CurationFileSchema.safeParse(curationRaw);
  if (!curationResult.success) {
    logger.fatal({ issues: curationResult.error.issues }, "curation.json is invalid");
    return 1;
  }

  const result = buildSnapshot(wgerExercises, curationResult.data);
  if (!result.ok) {
    logger.fatal(
      { problemCount: result.problems.length },
      "import found problems; nothing written",
    );
    for (const problem of result.problems) {
      logger.error(problem);
    }
    return 1;
  }

  // Safety net: the snapshot this script is about to commit must itself satisfy the schema that
  // `snapshot.test.ts` and the seeder parse it with.
  SnapshotFileSchema.parse(result.snapshot);

  await writeFile(SNAPSHOT_PATH, `${JSON.stringify(result.snapshot, null, 2)}\n`, "utf8");
  await writeFile(ATTRIBUTION_PATH, renderAttributionMarkdown(result.snapshot), "utf8");
  logger.info({ exercises: result.snapshot.exercises.length }, "snapshot and attribution written");

  if (reviewPath) {
    const wgerById = new Map(wgerExercises.map((exercise) => [exercise.id, exercise]));
    await writeFile(
      reviewPath,
      renderReviewTable(result.snapshot, curationResult.data, wgerById),
      "utf8",
    );
    logger.info({ reviewPath }, "review table written");
  }

  return 0;
}

main()
  .then((exitCode) => {
    process.exit(exitCode);
  })
  .catch((error: unknown) => {
    logger.fatal({ err: error }, "import failed");
    process.exit(1);
  });
