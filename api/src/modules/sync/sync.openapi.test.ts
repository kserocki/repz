import { describe, expect, it } from "vitest";
import { buildOpenApiDocument } from "../../shared/openapi.ts";
import { createTestApp } from "../../shared/testing/test-app.ts";

function generateDocument() {
  const { app } = createTestApp();
  return buildOpenApiDocument(app);
}

describe("OpenAPI document: /sync/*", () => {
  it("given the generated document, when inspected, then every push payload component exists", () => {
    const document = generateDocument();

    expect(document.components?.schemas).toMatchObject({
      WorkoutPayload: expect.any(Object),
      TemplatePayload: expect.any(Object),
      CustomExercisePayload: expect.any(Object),
      ExercisePayload: expect.any(Object),
      ExerciseTranslation: expect.any(Object),
    });
  });

  it("given the generated document, when the push request schema is inspected, then payload is oneOf the three payloads plus null", () => {
    const document = generateDocument();

    const requestBody = document.paths?.["/sync/push"]?.post?.requestBody as
      | { content?: Record<string, { schema?: unknown }> }
      | undefined;
    const pushSchema = requestBody?.content?.["application/json"]?.schema;
    expect(pushSchema).toBeDefined();

    // The request body schema references PushRequest; resolve it to inspect `changes.items.payload`.
    const pushRequestRef = (pushSchema as { $ref?: string }).$ref;
    const pushRequestSchema = pushRequestRef
      ? document.components?.schemas?.[pushRequestRef.replace("#/components/schemas/", "")]
      : pushSchema;
    const changesSchema = (pushRequestSchema as { properties?: Record<string, unknown> })
      ?.properties?.changes;
    const changeItemSchema = (changesSchema as { items?: Record<string, unknown> })?.items;
    const payloadSchema = (changeItemSchema as { properties?: Record<string, unknown> })?.properties
      ?.payload;

    expect(payloadSchema).toMatchObject({
      oneOf: [
        { $ref: "#/components/schemas/WorkoutPayload" },
        { $ref: "#/components/schemas/TemplatePayload" },
        { $ref: "#/components/schemas/CustomExercisePayload" },
        { type: "null" },
      ],
    });
  });

  it("given the generated document, when both sync paths are inspected, then each declares bearerAuth security", () => {
    const document = generateDocument();

    expect(document.paths?.["/sync/push"]?.post?.security).toEqual([{ bearerAuth: [] }]);
    expect(document.paths?.["/sync/pull"]?.get?.security).toEqual([{ bearerAuth: [] }]);
  });

  it("given the generated document, when the pull cursor parameter is inspected, then it is a non-null integer with minimum 0 and default 0", () => {
    const document = generateDocument();

    const parameters = document.paths?.["/sync/pull"]?.get?.parameters as
      | Array<{ name?: string; schema?: unknown }>
      | undefined;
    const cursorParameter = parameters?.find((parameter) => parameter.name === "cursor");

    expect(cursorParameter?.schema).toMatchObject({ type: "integer", minimum: 0, default: 0 });
  });

  it("given the generated document, when ExercisePayload is inspected, then sourceUrl is a nullable uri", () => {
    const document = generateDocument();

    const exercisePayload = document.components?.schemas?.ExercisePayload as
      | { properties?: Record<string, unknown> }
      | undefined;

    expect(exercisePayload?.properties?.sourceUrl).toEqual({
      type: ["string", "null"],
      format: "uri",
    });
  });
});
