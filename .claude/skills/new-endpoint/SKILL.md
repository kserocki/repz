---
name: new-endpoint
description: Add or change a Repz API endpoint in api/ (Hono + zod-openapi + Drizzle) and keep the OpenAPI contract, shared fixtures and Kotlin DTOs in sync. Use for any change to HTTP routes, request/response shapes or DB schema in api/.
---

# New / changed API endpoint

## Rule zero: never guess

If anything is unclear (naming, behaviour, UX copy, a token value, which module it belongs to, an API shape) or the docs disagree with the code, **stop and ask the user** before writing code. Do not invent values, screens, endpoints or animations that are not specified. Record approved assumptions in the docs (or an ADR) in the same change.

## Confirm first
- Method, path, auth (public / bearer), request and response shape, error cases (as Problem Details `type`s), and whether the mobile app needs it now. Write the contract as a Zod sketch and get a yes before implementing.

## Steps (api/)
1. **Schema:** Zod schemas in `src/modules/<module>/<module>.schemas.ts`, with `.openapi({ example })` on every schema.
2. **DB** (if needed): Drizzle table in `src/db/schema/`, then `pnpm db:generate` for the migration (never hand-edit generated SQL), then `pnpm db:migrate`. Syncable root tables need `user_id`, `version bigint` (from `sync_version_seq`), `updated_at` and `deleted_at`.
3. **Repository** (`*.repository.ts`): Drizzle queries only, no HTTP concerns.
4. **Service** (`*.service.ts`): business rules. Throw typed domain errors that are mapped to Problem Details in `shared/errors.ts`.
5. **Route** (`*.routes.ts`): `createRoute` + `app.openapi(...)`. Validation comes from the Zod schemas. Wire dependencies in `app.ts` (factory functions, no DI framework).
6. **Contract:** `pnpm openapi:export` updates `api/openapi.json` (committed). Add or update JSON fixtures in `contract/fixtures/`. The contract test validates them against the spec.
7. **Mobile side** (if consumed): hand-written `@Serializable` DTOs in `mobile/core/network`, matching the fixtures. Add a test that decodes every fixture. The API service is a Ktor function returning a typed result.
8. **Tests:** a Vitest unit test for the service. Route tests with the in-memory app (testcontainers tests are a later milestone).
9. Run the `verify` skill (TS part + mobile contract test).

## Don'ts
- No breaking change to an existing response without a version bump discussion with the user.
- No business logic in routes. No Drizzle in services.
- Never log tokens, passwords or full request bodies.
