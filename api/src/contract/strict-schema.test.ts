import { describe, expect, it } from "vitest";
import { buildOpenApiDocument } from "../shared/openapi.ts";
import { createTestApp } from "../shared/testing/test-app.ts";
import { createContractAjv } from "./ajv-setup.ts";
import { toStrictSchema } from "./strict-schema.ts";

function componentSchemas(): Record<string, unknown> {
  const { app } = createTestApp();
  const document = buildOpenApiDocument(app);
  return document.components?.schemas as Record<string, unknown>;
}

function compileStrict(schema: unknown, components: Record<string, unknown>) {
  const strict = toStrictSchema(schema, components);
  return createContractAjv().compile(strict);
}

describe("toStrictSchema", () => {
  it("given an object schema without additionalProperties, when a fixture has an extra field, then it is invalid", () => {
    const components = componentSchemas();
    const validate = compileStrict({ $ref: "#/components/schemas/User" }, components);
    const user = {
      id: "b6e6b6a0-1111-4a22-8a33-000000000001",
      email: "alex@example.com",
      displayName: "Alex",
      unitSystem: "kg",
    };

    expect(validate(user)).toBe(true);
    expect(validate({ ...user, extra: "nope" })).toBe(false);
  });

  it("given AuthSession's allOf, when a full session is checked, then it is valid and an extra field makes it invalid", () => {
    const components = componentSchemas();
    const validate = compileStrict({ $ref: "#/components/schemas/AuthSession" }, components);
    const session = {
      accessToken: "eyJhbGciOiJIUzI1NiJ9...",
      refreshToken: "3q2+7w==",
      expiresIn: 900,
      user: {
        id: "b6e6b6a0-1111-4a22-8a33-000000000001",
        email: "alex@example.com",
        displayName: "Alex",
        unitSystem: "kg",
      },
    };

    expect(validate(session)).toBe(true);
    expect(validate({ ...session, extra: "nope" })).toBe(false);
    // Merged from both allOf branches: every AuthTokens field plus user are required.
    const { refreshToken: _refreshToken, ...withoutRefreshToken } = session;
    expect(validate(withoutRefreshToken)).toBe(false);
  });

  it("given the push payload oneOf, when a workout payload is checked, then it matches exactly one branch", () => {
    const components = componentSchemas();
    const pushPayloadSchema = {
      oneOf: [
        { $ref: "#/components/schemas/WorkoutPayload" },
        { $ref: "#/components/schemas/TemplatePayload" },
        { $ref: "#/components/schemas/CustomExercisePayload" },
        { type: "null" },
      ],
    };
    const validate = compileStrict(pushPayloadSchema, components);

    const workoutPayload = {
      templateId: null,
      name: "Push day",
      notes: "",
      startedAt: "2026-11-01T10:00:00.000Z",
      finishedAt: "2026-11-01T10:45:00.000Z",
      exercises: [],
    };

    expect(validate(workoutPayload)).toBe(true);
    expect(validate(null)).toBe(true);
    expect(validate({ not: "any of the known payload shapes" })).toBe(false);
  });
});
