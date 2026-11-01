import type { ChangeHead, FieldError, PushOutcome, SyncStores, SyncType } from "../sync.types.ts";

export type ParsedChange = { deleted: boolean; aggregate: unknown };

export type SyncWriter = {
  parsePayload(
    raw: unknown,
  ): { ok: true; aggregate: unknown } | { ok: false; errors: FieldError[] };
  apply(stores: SyncStores, userId: string, id: string, change: ParsedChange): Promise<PushOutcome>;
};

export type SyncReader = {
  headsAfter(
    stores: SyncStores,
    userId: string,
    cursor: number,
    limit: number,
  ): Promise<ChangeHead[]>;
  countAfter(stores: SyncStores, userId: string, cursor: number): Promise<number>;
  /** Wire payloads, keyed by id; only ever asked for live (non-deleted) ids. */
  loadPayloads(stores: SyncStores, ids: string[]): Promise<Map<string, unknown>>;
};

/**
 * One entity's sync behaviour. `writer` is absent for a read-only handler (`catalogExercise`):
 * the catalog is seeded, never pushed to. Two handlers may share a `type` (`customExercise` and
 * `catalogExercise` both read `"exercise"`), but at most one of them may carry a `writer`.
 */
export type SyncHandler = {
  readonly name: "workout" | "template" | "customExercise" | "catalogExercise";
  readonly type: SyncType;
  readonly writer?: SyncWriter;
  readonly reader: SyncReader;
};

export type SyncHandlerRegistry = {
  readonly handlers: readonly SyncHandler[];
  /** The one writer registered for `type`. Throws if none is (every `SyncType` must have one). */
  writerFor(type: SyncType): SyncWriter;
};

/**
 * Built in `app.ts` from the four handlers (open/closed: a new entity is a new handler plus one
 * line in `app.ts`, never a change here or in the service). Fails fast on a duplicate handler
 * name or on two writers registered for the same type.
 */
export function createSyncHandlerRegistry(handlers: SyncHandler[]): SyncHandlerRegistry {
  const seenNames = new Set<string>();
  const writerTypes = new Set<SyncType>();

  for (const handler of handlers) {
    if (seenNames.has(handler.name)) {
      throw new Error(`Duplicate sync handler name: "${handler.name}"`);
    }
    seenNames.add(handler.name);

    if (handler.writer) {
      if (writerTypes.has(handler.type)) {
        throw new Error(`More than one sync writer registered for type: "${handler.type}"`);
      }
      writerTypes.add(handler.type);
    }
  }

  return {
    handlers,
    writerFor(type) {
      const handler = handlers.find((candidate) => candidate.type === type && candidate.writer);
      if (!handler?.writer) {
        throw new Error(`No sync writer registered for type: "${type}"`);
      }
      return handler.writer;
    },
  };
}
