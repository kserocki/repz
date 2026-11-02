import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { API_VERSION } from "./openapi.ts";
import { createTestApp } from "./testing/test-app.ts";

const OPENAPI_JSON_PATH = new URL("../../openapi.json", import.meta.url);
const PACKAGE_JSON_PATH = new URL("../../package.json", import.meta.url);

const DECLARED_TAGS = ["Health", "Auth", "Users", "Sync"];
const BEARER_AUTH_PATHS = new Set(["/me", "/sync/push", "/sync/pull"]);

type Operation = Record<string, unknown>;
type OperationEntry = { method: string; path: string; operation: Operation };

function readCommittedDocument(): Record<string, unknown> {
  return JSON.parse(readFileSync(OPENAPI_JSON_PATH, "utf8"));
}

function readPackageVersion(): string {
  const packageJson = JSON.parse(readFileSync(PACKAGE_JSON_PATH, "utf8")) as { version: string };
  return packageJson.version;
}

/** Every method entry (`get`, `post`, ...) under every path, flattened for cross-operation checks. */
function listOperations(document: Record<string, unknown>): OperationEntry[] {
  const paths = document.paths as Record<string, Record<string, Operation>>;
  return Object.entries(paths).flatMap(([path, pathItem]) =>
    Object.entries(pathItem).map(([method, operation]) => ({ method, path, operation })),
  );
}

function declaresBearerAuth(operation: Operation): boolean {
  const security = operation.security as Array<Record<string, unknown>> | undefined;
  return security?.some((requirement) => "bearerAuth" in requirement) ?? false;
}

describe("GET /openapi.json", () => {
  it("given the running app, when requested, then the body deep-equals the committed api/openapi.json", async () => {
    const { app } = createTestApp();

    const res = await app.request("/openapi.json");

    expect(res.status).toBe(200);
    expect(await res.json()).toEqual(readCommittedDocument());
  });
});

describe("the committed OpenAPI document", () => {
  it("given the document, when inspected, then it declares OpenAPI 3.1.0 and the package's version", () => {
    const document = readCommittedDocument();
    const info = document.info as { version: string };

    expect(document.openapi).toBe("3.1.0");
    expect(info.version).toBe(API_VERSION);
    expect(API_VERSION).toBe(readPackageVersion());
  });

  it("given every operation, when inspected, then operationId is unique, every tag is declared and summary is non-empty", () => {
    const operations = listOperations(readCommittedDocument());
    expect(operations).toHaveLength(9);

    const operationIds = operations.map((entry) => entry.operation.operationId);
    expect(new Set(operationIds).size).toBe(operationIds.length);
    for (const operationId of operationIds) {
      expect(typeof operationId).toBe("string");
      expect(operationId).not.toHaveLength(0);
    }

    for (const entry of operations) {
      const tags = entry.operation.tags as string[] | undefined;
      expect(tags?.length).toBeGreaterThan(0);
      for (const tag of tags ?? []) {
        expect(DECLARED_TAGS).toContain(tag);
      }

      expect(typeof entry.operation.summary).toBe("string");
      expect(entry.operation.summary).not.toHaveLength(0);
    }
  });

  it("given the components, when bearerAuth is inspected, then it is an HTTP bearer JWT scheme", () => {
    const document = readCommittedDocument();
    const components = document.components as { securitySchemes?: Record<string, unknown> };

    expect(components.securitySchemes?.bearerAuth).toEqual({
      type: "http",
      scheme: "bearer",
      bearerFormat: "JWT",
    });
  });

  it("given every operation, when inspected, then bearerAuth security is declared exactly on /me and /sync/*", () => {
    const operations = listOperations(readCommittedDocument());

    for (const entry of operations) {
      expect(declaresBearerAuth(entry.operation)).toBe(BEARER_AUTH_PATHS.has(entry.path));
    }
  });

  it("given paths, when inspected, then /openapi.json and /docs are absent", () => {
    const document = readCommittedDocument();
    const paths = document.paths as Record<string, unknown>;

    expect(paths["/openapi.json"]).toBeUndefined();
    expect(paths["/docs"]).toBeUndefined();
  });
});

describe("GET /docs", () => {
  it("given the running app, when requested, then it responds 200 text/html referencing /openapi.json", async () => {
    const { app } = createTestApp();

    const res = await app.request("/docs");
    const body = await res.text();

    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toContain("text/html");
    expect(body).toContain("/openapi.json");
  });

  it("given the running app, when requested, then it pins the CDN bundle and disables telemetry, developer tools and the agent", async () => {
    const { app } = createTestApp();

    const res = await app.request("/docs");
    const body = await res.text();

    expect(body).toContain(
      '<script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference@1.76.4"></script>',
    );
    expect(body).toContain('"telemetry": false');
    expect(body).toContain('"showDeveloperTools": "never"');
    expect(body).toContain('"disabled": true');
  });
});
