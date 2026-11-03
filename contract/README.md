# Repz API contract fixtures

Shared JSON fixtures validated against `api/openapi.json` by the API's contract test
(`api/src/contract/contract-fixtures.test.ts`, run in `pnpm test`). Mobile's `core:network`
decodes the same fixtures into Kotlin DTOs (see "Mobile consumer" below), so a change here must
stay accurate on both sides.

## Layout

```
contract/fixtures/<operationId>/request/<case>.json     # a request body
contract/fixtures/<operationId>/<status>/<case>.json     # a response body, one status folder per code
contract/fixtures/components/<SchemaName>/<case>.json    # a response-only component (e.g. ProblemDetails)
```

`<operationId>` is the OpenAPI `operationId` (e.g. `syncPush`, `getMe`), exactly as declared on the
route in `api/src/modules/*/*.routes.ts`. `<status>` is the plain numeric status code (`200`,
`400`, ...). `<case>` is a short, descriptive, kebab-case name for that example (`valid`,
`validation-error`, `accepted-insert`, ...); it never appears in the validated shape, only in the
file name.

Content type is implied, never stated in the path: `request` and every 2xx response are
`application/json`; every 4xx/5xx response is `application/problem+json`. 204 responses (logout)
carry no body and have no fixture.

## Mobile consumer

`core:network`'s `ContractFixturesTest` (`mobile/core/network/src/commonTest/kotlin/com/repz/app/core/network/contract/ContractFixturesTest.kt`)
decodes every fixture here with the module's strict Kotlin DTOs, on both the Android host and the
iOS simulator, and checks coverage both ways against its own catalog of operations. Kotlin/Native
has no resource bundle to read these files from at test time, so the `generateContractFixtures`
Gradle task (`mobile/build-logic/convention/src/main/kotlin/com/repz/buildlogic/contract/GenerateContractFixturesTask.kt`,
wired into `core:network`'s `build.gradle.kts`) walks `contract/fixtures` and bakes every file into
a generated Kotlin source as string literals, added to `commonTest`. Decoding is strict there too
(unknown fields and enum values fail, matching the fixtures' own `additionalProperties: false`),
and every request fixture is decoded and re-encoded to confirm it round-trips to the exact same
wire JSON. A fixture added or changed here must keep decoding clean on both platforms.

## Rules

- **Each file is the raw JSON body, exactly as sent or received on the wire.** No envelope, no
  metadata alongside it — the path carries that.
- **Every fixture is captured, never typed from memory.** Run the real request through
  `createTestApp()` (see `api/src/modules/*/*.routes.test.ts` for the pattern), read the actual
  response, and write that out. IDs and the request id are then normalised to stable, readable
  UUIDs (e.g. `b6e6b6a0-1111-4a22-8a33-000000000001`) so the committed fixture reads well and never
  changes for no reason on a re-capture.
- **Formatting:** 2 spaces, a trailing newline, nothing else. The contract test checks this byte
  for byte (`JSON.stringify(JSON.parse(raw), null, 2) + "\n"`).
- **Strict:** every fixture is validated with `additionalProperties: false` added to every object
  schema that doesn't already declare it, so a field the spec doesn't document fails the fixture,
  not just a field the fixture is missing.
- **Coverage both ways:** every documented request body and every documented response status with
  content must have at least one fixture; every fixture must resolve to something documented. An
  undocumented folder (wrong `operationId`, an undeclared status, a typo) fails loudly, naming the
  fixture.

## Adding a fixture

1. Write (or extend) a test in the relevant `api/src/modules/*/*.routes.test.ts` that drives the
   exact scenario through `createTestApp()`.
2. Run it, copy the real response (or request) body, and save it under the right path above.
3. Format it: 2-space JSON, trailing newline. Normalise any freshly generated id/requestId to a
   stable UUID if it isn't already one.
4. Run `pnpm test` from `api/`: the contract test validates the new fixture against the spec and
   checks coverage both ways.
