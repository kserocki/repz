import { readdirSync, readFileSync } from "node:fs";
import { join, relative, sep } from "node:path";

export type FixtureTarget =
  | { kind: "request"; operationId: string }
  | { kind: "response"; operationId: string; status: number }
  | { kind: "component"; schemaName: string };

export type ContractFixture = {
  relativePath: string;
  target: FixtureTarget;
  body: unknown;
  raw: string;
};

function listJsonFiles(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      return listJsonFiles(entryPath);
    }
    return entry.isFile() && entry.name.endsWith(".json") ? [entryPath] : [];
  });
}

/**
 * `<operationId>/request/<case>.json`, `<operationId>/<status>/<case>.json` or
 * `components/<SchemaName>/<case>.json` (§3.3). Only the directory shape is parsed here; whether
 * the target is actually documented is the contract test's job.
 */
function parseTarget(relativePath: string): FixtureTarget {
  const segments = relativePath.split(sep);
  const [first, second] = segments;
  if (segments.length === 3 && first === "components" && second) {
    return { kind: "component", schemaName: second };
  }
  if (segments.length === 3 && first && second === "request") {
    return { kind: "request", operationId: first };
  }
  if (segments.length === 3 && first && second && /^\d+$/.test(second)) {
    return { kind: "response", operationId: first, status: Number(second) };
  }
  throw new Error(
    `Unrecognized fixture path (expected "<id>/request/<case>.json", "<id>/<status>/<case>.json" ` +
      `or "components/<Name>/<case>.json"): ${relativePath}`,
  );
}

/** Walks `fixturesRoot` (the repo's `contract/fixtures` directory) and loads every `.json` fixture. */
export function loadContractFixtures(fixturesRoot: string): ContractFixture[] {
  return listJsonFiles(fixturesRoot)
    .sort()
    .map((filePath) => {
      const relativePath = relative(fixturesRoot, filePath);
      const raw = readFileSync(filePath, "utf8");
      return { relativePath, target: parseTarget(relativePath), body: JSON.parse(raw), raw };
    });
}
