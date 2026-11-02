import { afterEach, describe, expect, it, vi } from "vitest";
import { type FetchLike, fetchAllExerciseInfo, WGER_IMPORT_USER_AGENT } from "./wger-client.ts";

function page(results: unknown[], next: string | null) {
  return { count: results.length, next, results };
}

function minimalWgerExercise(id: number) {
  return {
    id,
    uuid: `uuid-${id}`,
    muscles: [{ id: 4 }],
    muscles_secondary: [],
    equipment: [{ id: 1 }],
    license: { short_name: "CC-BY-SA 3" },
    license_author: "author",
    translations: [
      {
        language: 2,
        name: `Exercise ${id}`,
        description: "<p>Text</p>",
        license: 1,
        license_author: "author",
        author_history: [],
      },
    ],
  };
}

function jsonResponse(body: unknown) {
  return { ok: true, status: 200, statusText: "OK", json: async () => body };
}

afterEach(() => {
  vi.useRealTimers();
});

describe("fetchAllExerciseInfo", () => {
  it("given a paginated response, when fetched, then it follows next until exhausted", async () => {
    vi.useFakeTimers();
    const calls: string[] = [];
    const fetchImpl: FetchLike = async (url) => {
      calls.push(url);
      if (url.includes("offset")) {
        return jsonResponse(page([minimalWgerExercise(2)], null));
      }
      return jsonResponse(
        page([minimalWgerExercise(1)], "https://wger.de/api/v2/exerciseinfo/?limit=100&offset=100"),
      );
    };

    const promise = fetchAllExerciseInfo(fetchImpl, "https://wger.de/api/v2");
    await vi.runAllTimersAsync();
    const results = await promise;

    expect(results.map((exercise) => exercise.id)).toEqual([1, 2]);
    expect(calls).toEqual([
      "https://wger.de/api/v2/exerciseinfo/?limit=100",
      "https://wger.de/api/v2/exerciseinfo/?limit=100&offset=100",
    ]);
  });

  it("given every request, when fetched, then it sends the Repz User-Agent", async () => {
    const seenHeaders: Array<Record<string, string> | undefined> = [];
    const fetchImpl: FetchLike = async (_url, init) => {
      seenHeaders.push(init?.headers);
      return jsonResponse(page([], null));
    };

    await fetchAllExerciseInfo(fetchImpl, "https://wger.de/api/v2");

    expect(seenHeaders).toHaveLength(1);
    expect(seenHeaders[0]?.["User-Agent"]).toBe(WGER_IMPORT_USER_AGENT);
  });

  it("given a non-200 response, when fetched, then it throws naming the status", async () => {
    const fetchImpl: FetchLike = async () => ({
      ok: false,
      status: 503,
      statusText: "Service Unavailable",
      json: async () => ({}),
    });

    await expect(fetchAllExerciseInfo(fetchImpl, "https://wger.de/api/v2")).rejects.toThrow(/503/);
  });

  it("given a response whose shape no longer matches what we read, when fetched, then Zod rejects it", async () => {
    const fetchImpl: FetchLike = async () =>
      jsonResponse({ count: 1, next: null, results: [{ id: 1, name: "missing required fields" }] });

    await expect(fetchAllExerciseInfo(fetchImpl, "https://wger.de/api/v2")).rejects.toThrow();
  });
});
