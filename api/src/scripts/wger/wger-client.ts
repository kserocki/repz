import { type WgerExerciseInfo, WgerExerciseInfoPageSchema } from "./wger.schemas.ts";

/** Identifies Repz to wger.de, per their request to API consumers. */
export const WGER_IMPORT_USER_AGENT = "repz-import-wger (+https://github.com/kserocki/repz)";

const PAGE_LIMIT = 100;
const DELAY_BETWEEN_PAGES_MS = 1000;

export type FetchLike = (
  input: string,
  init?: { headers?: Record<string, string> },
) => Promise<{ ok: boolean; status: number; statusText: string; json(): Promise<unknown> }>;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Pages through `GET {baseUrl}/exerciseinfo/?limit=100`, one request at a time with a 1 s pause
 * between pages — politeness to wger.de, a free community-run service, matching their own
 * throttling notes — and a User-Agent that identifies Repz. Every page is Zod-validated against
 * the subset of the shape we read (`wger.schemas.ts`), so an upstream shape change fails loudly
 * here instead of silently importing garbage.
 */
export async function fetchAllExerciseInfo(
  fetchImpl: FetchLike,
  baseUrl: string,
): Promise<WgerExerciseInfo[]> {
  const results: WgerExerciseInfo[] = [];
  let url: string | null = `${baseUrl}/exerciseinfo/?limit=${PAGE_LIMIT}`;
  let isFirstPage = true;

  while (url) {
    if (!isFirstPage) {
      await sleep(DELAY_BETWEEN_PAGES_MS);
    }
    isFirstPage = false;

    const response = await fetchImpl(url, {
      headers: { "User-Agent": WGER_IMPORT_USER_AGENT, Accept: "application/json" },
    });
    if (!response.ok) {
      throw new Error(
        `wger API request to ${url} failed: ${response.status} ${response.statusText}`,
      );
    }
    const page = WgerExerciseInfoPageSchema.parse(await response.json());
    results.push(...page.results);
    url = page.next;
  }

  return results;
}
