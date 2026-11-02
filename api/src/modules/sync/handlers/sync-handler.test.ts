import { describe, expect, it } from "vitest";
import { createSyncHandlerRegistry, type SyncHandler } from "./sync-handler.ts";

function readerOnlyHandler(name: SyncHandler["name"], type: SyncHandler["type"]): SyncHandler {
  return {
    name,
    type,
    reader: {
      headsAfter: async () => [],
      countAfter: async () => 0,
      loadPayloads: async () => new Map(),
    },
  };
}

function writerHandler(name: SyncHandler["name"], type: SyncHandler["type"]): SyncHandler {
  return {
    ...readerOnlyHandler(name, type),
    writer: {
      parsePayload: () => ({ ok: true, aggregate: {} }),
      apply: async () => ({ kind: "accepted", version: 1, deleted: false }),
    },
  };
}

describe("createSyncHandlerRegistry", () => {
  it("given one handler per type, when built, then it exposes every handler and resolves each writer", () => {
    const workout = writerHandler("workout", "workout");
    const template = writerHandler("template", "template");
    const customExercise = writerHandler("customExercise", "exercise");
    const catalogExercise = readerOnlyHandler("catalogExercise", "exercise");

    const registry = createSyncHandlerRegistry([
      workout,
      template,
      customExercise,
      catalogExercise,
    ]);

    expect(registry.handlers).toHaveLength(4);
    expect(registry.writerFor("workout")).toBe(workout.writer);
    expect(registry.writerFor("exercise")).toBe(customExercise.writer);
  });

  it("given two writers registered for the same type, when built, then it throws", () => {
    const first = writerHandler("customExercise", "exercise");
    const second: SyncHandler = { ...writerHandler("catalogExercise", "exercise") };

    expect(() => createSyncHandlerRegistry([first, second])).toThrow(/writer/i);
  });

  it("given two handlers with the same name, when built, then it throws", () => {
    const handlers = [
      writerHandler("workout", "workout"),
      readerOnlyHandler("workout", "template"),
    ];

    expect(() => createSyncHandlerRegistry(handlers)).toThrow(/duplicate/i);
  });

  it("given a type with no writer registered, when writerFor is called, then it throws", () => {
    const registry = createSyncHandlerRegistry([readerOnlyHandler("catalogExercise", "exercise")]);

    expect(() => registry.writerFor("exercise")).toThrow(/no sync writer/i);
  });
});
