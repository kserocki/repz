import { readFileSync } from "node:fs";
import type { OpenAPIHono } from "@hono/zod-openapi";
import type { AppEnv } from "./app-env.ts";

type PackageManifest = { version: string };

const packageManifest = JSON.parse(
  readFileSync(new URL("../../package.json", import.meta.url), "utf8"),
) as PackageManifest;

/** The single source of truth for the API's version: `package.json#version`. */
export const API_VERSION = packageManifest.version;

type OpenApiDocumentConfig = Parameters<OpenAPIHono["getOpenAPI31Document"]>[0];
type OpenApiDocument = ReturnType<OpenAPIHono["getOpenAPI31Document"]>;

/**
 * Shared by the served `/openapi.json` and `pnpm openapi:export`, so both can never drift from
 * each other.
 */
export const OPENAPI_DOCUMENT_CONFIG: OpenApiDocumentConfig = {
  openapi: "3.1.0",
  info: {
    title: "Repz API",
    version: API_VERSION,
    description: "The Repz backend: auth, the signed-in user's profile and offline-first sync.",
  },
  servers: [{ url: "http://localhost:3000", description: "Local API (pnpm dev)" }],
  tags: [
    { name: "Health", description: "Liveness." },
    { name: "Auth", description: "Registration, sign-in and the refresh/access token lifecycle." },
    { name: "Users", description: "The signed-in user's profile." },
    { name: "Sync", description: "Offline-first push/pull synchronization." },
  ],
};

/** Builds the OpenAPI 3.1 document from `app`'s registry (every route already mounted). */
export function buildOpenApiDocument(app: OpenAPIHono<AppEnv>): OpenApiDocument {
  return app.getOpenAPI31Document(OPENAPI_DOCUMENT_CONFIG);
}

/** 2-space JSON plus a trailing newline: the exact bytes committed to `api/openapi.json`. */
export function serializeOpenApiDocument(document: OpenApiDocument): string {
  return `${JSON.stringify(document, null, 2)}\n`;
}
