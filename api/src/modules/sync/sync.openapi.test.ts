import { describe, expect, it } from "vitest";
import { createTestApp } from "../../shared/testing/test-app.ts";

function generateDocument() {
  const { app } = createTestApp();
  return app.getOpenAPIDocument({ openapi: "3.0.0", info: { title: "Repz API", version: "test" } });
}

describe("OpenAPI document: /sync/*", () => {
  it("given the generated document, when inspected, then every push payload component exists", () => {
    const document = generateDocument();

    expect(document.components?.schemas).toMatchObject({
      WorkoutPayload: expect.any(Object),
      TemplatePayload: expect.any(Object),
      CustomExercisePayload: expect.any(Object),
      ExercisePayload: expect.any(Object),
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
});
