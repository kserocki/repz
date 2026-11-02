import { readFileSync } from "node:fs";
import { Validator } from "@seriousme/openapi-schema-validator";
import { describe, expect, it } from "vitest";
import { createContractAjv } from "./ajv-setup.ts";
import {
  type ContractFixture,
  type FixtureTarget,
  loadContractFixtures,
} from "./fixture-catalog.ts";
import { toStrictSchema } from "./strict-schema.ts";

const OPENAPI_JSON_PATH = new URL("../../openapi.json", import.meta.url);
const FIXTURES_ROOT = new URL("../../../contract/fixtures/", import.meta.url);

type Operation = Record<string, unknown>;
type OperationEntry = { path: string; operation: Operation };
type RequiredTarget =
  | { kind: "request"; operationId: string }
  | { kind: "response"; operationId: string; status: number };

function readCommittedDocument(): Record<string, unknown> {
  return JSON.parse(readFileSync(OPENAPI_JSON_PATH, "utf8"));
}

function operationsByOperationId(document: Record<string, unknown>): Map<string, OperationEntry> {
  const paths = document.paths as Record<string, Record<string, Operation>>;
  const map = new Map<string, OperationEntry>();
  for (const [path, pathItem] of Object.entries(paths)) {
    for (const operation of Object.values(pathItem)) {
      if (typeof operation.operationId === "string") {
        map.set(operation.operationId, { path, operation });
      }
    }
  }
  return map;
}

function hasContent(entry: { content?: Record<string, unknown> } | undefined): boolean {
  return Object.keys(entry?.content ?? {}).length > 0;
}

/** Content type is implied by the target (§3.3): request/2xx is JSON, 4xx/5xx is Problem Details. */
function contentTypeFor(status: number): string {
  return status >= 200 && status < 300 ? "application/json" : "application/problem+json";
}

function resolveTargetSchema(
  document: Record<string, unknown>,
  operations: Map<string, OperationEntry>,
  target: FixtureTarget,
): unknown | undefined {
  if (target.kind === "component") {
    const schemas = (document.components as { schemas?: Record<string, unknown> } | undefined)
      ?.schemas;
    return schemas?.[target.schemaName];
  }

  const entry = operations.get(target.operationId);
  if (!entry) {
    return undefined;
  }

  if (target.kind === "request") {
    const requestBody = entry.operation.requestBody as
      | { content?: Record<string, { schema?: unknown }> }
      | undefined;
    if (!hasContent(requestBody)) {
      return undefined;
    }
    return requestBody?.content?.["application/json"]?.schema;
  }

  const responses = entry.operation.responses as
    | Record<string, { content?: Record<string, { schema?: unknown }> }>
    | undefined;
  const response = responses?.[String(target.status)];
  if (!hasContent(response)) {
    return undefined;
  }
  return response?.content?.[contentTypeFor(target.status)]?.schema;
}

/** Every documented request body and response-with-content: the fixtures must cover all of them. */
function requiredTargets(operations: Map<string, OperationEntry>): RequiredTarget[] {
  const targets: RequiredTarget[] = [];
  for (const [operationId, entry] of operations) {
    const requestBody = entry.operation.requestBody as
      | { content?: Record<string, unknown> }
      | undefined;
    if (hasContent(requestBody)) {
      targets.push({ kind: "request", operationId });
    }

    const responses = entry.operation.responses as
      | Record<string, { content?: Record<string, unknown> }>
      | undefined;
    for (const [statusText, response] of Object.entries(responses ?? {})) {
      if (hasContent(response)) {
        targets.push({ kind: "response", operationId, status: Number(statusText) });
      }
    }
  }
  return targets;
}

function sameTarget(fixtureTarget: FixtureTarget, required: RequiredTarget): boolean {
  if (fixtureTarget.kind !== required.kind) {
    return false;
  }
  if (fixtureTarget.kind === "request" && required.kind === "request") {
    return fixtureTarget.operationId === required.operationId;
  }
  if (fixtureTarget.kind === "response" && required.kind === "response") {
    return (
      fixtureTarget.operationId === required.operationId && fixtureTarget.status === required.status
    );
  }
  return false;
}

function describeTarget(target: FixtureTarget | RequiredTarget): string {
  switch (target.kind) {
    case "request":
      return `request body for operation "${target.operationId}"`;
    case "response":
      return `${target.status} response for operation "${target.operationId}"`;
    case "component":
      return `component schema "${target.schemaName}"`;
  }
}

const document = readCommittedDocument();
const operations = operationsByOperationId(document);
const components = (document.components as { schemas?: Record<string, unknown> } | undefined)
  ?.schemas;
const fixtures = loadContractFixtures(FIXTURES_ROOT.pathname);
if (fixtures.length === 0) {
  throw new Error(`No fixtures found under ${FIXTURES_ROOT.pathname}`);
}

const ajv = createContractAjv();

describe("api/openapi.json", () => {
  it("given the committed document, when validated against the OAS 3.1 meta-schema, then it is valid", async () => {
    const validator = new Validator();
    const result = await validator.validate(document);

    expect(result.valid, JSON.stringify(result.errors)).toBe(true);
    expect(validator.version).toBe("3.1");
  });
});

describe("contract fixture coverage", () => {
  it("given every documented request body and response with content, when checked, then at least one fixture targets it", () => {
    for (const required of requiredTargets(operations)) {
      const covered = fixtures.some((fixture) => sameTarget(fixture.target, required));
      expect(covered, `missing a fixture for the ${describeTarget(required)}`).toBe(true);
    }
  });

  it("given every fixture, when its target is resolved, then it names a documented request, response or component", () => {
    for (const fixture of fixtures) {
      const schema = resolveTargetSchema(document, operations, fixture.target);
      expect(
        schema,
        `${fixture.relativePath} targets an undocumented ${describeTarget(fixture.target)}`,
      ).toBeDefined();
    }
  });
});

describe("contract fixtures", () => {
  it.each(fixtures)(
    "given $relativePath, when formatted, then it is 2-space JSON with a trailing newline",
    (fixture: ContractFixture) => {
      const canonical = `${JSON.stringify(JSON.parse(fixture.raw), null, 2)}\n`;
      expect(fixture.raw).toBe(canonical);
    },
  );

  it.each(fixtures)(
    "given $relativePath, when validated against its schema, then it is valid",
    (fixture: ContractFixture) => {
      const schema = resolveTargetSchema(document, operations, fixture.target);
      if (!schema) {
        throw new Error(
          `${fixture.relativePath} targets an undocumented ${describeTarget(fixture.target)}`,
        );
      }

      const strictSchema = toStrictSchema(schema, components ?? {});
      const validate = ajv.compile(strictSchema);
      const valid = validate(fixture.body);

      if (!valid) {
        const details = (validate.errors ?? [])
          .map((error) => `${error.instancePath || "(root)"} ${error.message}`)
          .join("; ");
        throw new Error(`${fixture.relativePath} failed schema validation: ${details}`);
      }
      expect(valid).toBe(true);
    },
  );
});
