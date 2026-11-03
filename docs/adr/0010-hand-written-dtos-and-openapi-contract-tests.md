# ADR-0010: Hand-written DTOs + OpenAPI contract tests

- **Status:** Accepted (2026-10-28)

## Context
The API is small and fully ours. Generated Kotlin clients are noisy.

## Decision
The API publishes OpenAPI generated from Zod (`api/openapi.json`, committed). Kotlin DTOs are hand-written. Shared JSON fixtures are validated against the spec in CI on both sides.

## Consequences
Clean code, but discipline is needed: every API change updates the spec, the fixtures and the DTOs (the `new-endpoint` skill).

## Amendment (REP-17, 2026-11-02)
- **The spec is OpenAPI 3.1.0.** The push payload `oneOf [..., { type: "null" }]` is only valid in 3.1.
- **`pnpm openapi:export`** writes the committed `api/openapi.json`.
  - It needs no database (it builds the in-memory app), keeps the generator's insertion order and is Biome-ignored.
  - The app serves the same document at `/openapi.json`, and a test checks that the served spec deep-equals the committed file.
  - CI runs the export before its clean-tree check, so any spec drift fails the build.
- **Fixtures** live in `contract/fixtures/<operationId>/{request|<status>}/<case>.json`, plus `components/<Schema>/` for global problems such as the 404 and the 500. Rules:
  - each file is the raw wire body, **captured** from the in-memory app, never typed by hand;
  - formatting is fixed: 2 spaces and a trailing newline;
  - the contract test validates fixtures **strictly**: it adds `additionalProperties: false` to every object schema, so an undocumented field fails;
  - coverage is checked both ways: every documented request body, response status and response-only component has a fixture, and every fixture folder matches a documented request, response or component.
- **Contract test:** Ajv in 2020-12 mode plus ajv-formats. The spec itself is validated against the official OAS 3.1 schema.
- **`/docs`:** Scalar, with the browser bundle pinned to an exact version, no proxy, no telemetry, the developer tools and the AI agent turned off (they would send the spec to Scalar's services); default fonts load from Scalar's font CDN. The script loads without SRI, which is acceptable for a local-only API. It is served in every environment, because the API is local-only (ADR-0001) and the spec is public in the repo.
- **Mobile side:** the `core:network` test that decodes every fixture into the hand-written DTOs arrives with REP-25.

## Amendment (REP-25, 2026-11-03): the mobile half

- **Fixtures reach Kotlin/Native as code, not resources.** `GenerateContractFixturesTask` (`mobile/build-logic/convention`, registered via `registerGenerateContractFixtures()`) reads every file under `contract/fixtures/**` and writes `GeneratedContractFixtures.kt` into `core:network`'s `commonTest` source set, because a Kotlin/Native simulator test process has no resource bundle or classpath file I/O. `ContractFixturesTest` (`core:network/src/commonTest/.../contract/`) then decodes every fixture.
- **`StrictContractJson`** (`core:network/.../client/NetworkJson.kt`) is the production `NetworkJson` config with `ignoreUnknownKeys = false`, so a fixture carrying a field the DTOs don't know about fails to decode instead of silently passing.
- **Coverage is checked both ways:** every generated fixture has a matching catalog entry, and every catalog entry (`CONTRACT_CATALOG`) has at least one fixture; either gap fails the test by name.
- **Request fixtures round-trip byte-exact:** re-encoding a decoded request fixture must produce the identical wire JSON. That needed two hand-written serializers: `WireInstantSerializer` (UTC, exactly 3 fractional digits, `.000Z`, matching ADR-0020's millisecond precision) and `WireDoubleSerializer` (a whole-number double encodes with no decimal point, e.g. `8` not `8.0`, matching the JS server's own `Number#toString()`).
- **`mobile.yml`'s path filter includes `contract/**`,** so a fixture-only change still runs the mobile CI jobs.
- **Production decoding tolerates additive fields, not additive vocabulary.** `NetworkJson` sets `ignoreUnknownKeys = true`, so a new, as-yet-unused server field reaching an older installed build doesn't crash it. Enum/vocabulary values (muscle, equipment, set type, unit system, sync type) stay strict either way: a hand-written vocabulary serializer has no "unknown" fallback to coerce an unrecognized value into, so decoding still fails and that pull page is retried on the next sync run (Q2).
