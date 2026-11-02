import type pino from "pino";
import type { SyncHandlerRegistry } from "./handlers/sync-handler.ts";
import { SyncIdConflict } from "./sync.errors.ts";
import type {
  FieldError,
  PullPage,
  PullTotalEstimate,
  PushOutcome,
  SyncTransactions,
  SyncType,
} from "./sync.types.ts";
import { SYNC_TYPES } from "./sync.types.ts";

/** `{ total: 0, workout: 0, template: 0, exercise: 0 }`, derived so a new `SyncType` needs no edit here. */
function emptyTotalEstimate(): PullTotalEstimate {
  const byType = Object.fromEntries(SYNC_TYPES.map((type) => [type, 0])) as Record<
    SyncType,
    number
  >;
  return { total: 0, ...byType };
}

export type PushEnvelope = { type: SyncType; id: string; deleted: boolean; payload?: unknown };

export type ItemProblemWire = { type: string; title: string; errors?: FieldError[] };

export type PushResult = {
  accepted: Array<{ type: SyncType; id: string; version: number | null; deleted: boolean }>;
  rejected: Array<{ type: SyncType; id: string; problem: ItemProblemWire }>;
};

export type SyncService = {
  push(userId: string, changes: PushEnvelope[]): Promise<PushResult>;
  pull(userId: string, cursor: number, limit: number): Promise<PullPage>;
};

type ProblemKind = "validation-error" | "read-only" | "id-conflict" | "invalid-reference";

const PROBLEM_TITLES: Record<ProblemKind, string> = {
  "validation-error": "Validation Error",
  "read-only": "Read Only",
  "id-conflict": "Id Conflict",
  "invalid-reference": "Invalid Reference",
};

function toItemProblem(kind: ProblemKind, errors?: FieldError[]): ItemProblemWire {
  return {
    type: `urn:repz:problem:${kind}`,
    title: PROBLEM_TITLES[kind],
    ...(errors ? { errors } : {}),
  };
}

type ParsedItem =
  | { index: number; change: PushEnvelope; ok: true; aggregate: unknown }
  | { index: number; change: PushEnvelope; ok: false; errors: FieldError[] };

export function createSyncService(deps: {
  transactions: SyncTransactions;
  registry: SyncHandlerRegistry;
  logger: pino.Logger;
}): SyncService {
  return {
    async push(userId, changes) {
      const parsedItems: ParsedItem[] = changes.map((change, index) => {
        if (change.deleted) {
          return { index, change, ok: true, aggregate: null };
        }
        const writer = deps.registry.writerFor(change.type);
        const result = writer.parsePayload(change.payload);
        return result.ok
          ? { index, change, ok: true, aggregate: result.aggregate }
          : { index, change, ok: false, errors: result.errors };
      });

      const toApply = parsedItems.filter(
        (item): item is Extract<ParsedItem, { ok: true }> => item.ok,
      );

      const outcomeByIndex = new Map<number, PushOutcome>();

      // A push whose every item fails payload validation needs no transaction and takes no lock,
      // since nothing is written.
      if (toApply.length > 0) {
        const applied = await deps.transactions.write(async (stores) => {
          await stores.locks.lockUserSyncWrites(userId);

          const results = new Map<number, PushOutcome>();
          for (const item of toApply) {
            const writer = deps.registry.writerFor(item.change.type);
            try {
              // One savepoint per change: a constraint failure rolls back only this item.
              const outcome = await stores.savepoint((savepointStores) =>
                writer.apply(savepointStores, userId, item.change.id, {
                  deleted: item.change.deleted,
                  aggregate: item.aggregate,
                }),
              );
              results.set(item.index, outcome);
            } catch (error) {
              if (error instanceof SyncIdConflict) {
                results.set(item.index, { kind: "rejected", problem: "id-conflict" });
              } else {
                throw error;
              }
            }
          }
          return results;
        });
        applied.forEach((outcome, index) => {
          outcomeByIndex.set(index, outcome);
        });
      }

      const accepted: PushResult["accepted"] = [];
      const rejected: PushResult["rejected"] = [];

      for (const item of parsedItems) {
        const { change } = item;
        if (!item.ok) {
          rejected.push({
            type: change.type,
            id: change.id,
            problem: toItemProblem("validation-error", item.errors),
          });
          continue;
        }
        const outcome = outcomeByIndex.get(item.index);
        if (!outcome) {
          throw new Error("Missing push outcome for an applied change");
        }
        if (outcome.kind === "accepted") {
          accepted.push({
            type: change.type,
            id: change.id,
            version: outcome.version,
            deleted: outcome.deleted,
          });
        } else {
          rejected.push({
            type: change.type,
            id: change.id,
            problem: toItemProblem(outcome.problem, outcome.errors),
          });
        }
      }

      const rejectionCounts: Record<string, number> = {};
      for (const item of rejected) {
        rejectionCounts[item.problem.type] = (rejectionCounts[item.problem.type] ?? 0) + 1;
      }
      deps.logger.info(
        { userId, accepted: accepted.length, rejected: rejected.length, rejectionCounts },
        "sync push applied",
      );

      return { accepted, rejected };
    },

    async pull(userId, cursor, limit) {
      return deps.transactions.readSnapshot(async (stores) => {
        const headsByHandler = await Promise.all(
          deps.registry.handlers.map(async (handler) => {
            const heads = await handler.reader.headsAfter(stores, userId, cursor, limit + 1);
            return heads.map((head) => ({ ...head, handler }));
          }),
        );
        const allHeads = headsByHandler.flat().sort((a, b) => a.version - b.version);
        const hasMore = allHeads.length > limit;
        const page = allHeads.slice(0, limit);

        const idsByHandler = new Map<(typeof deps.registry.handlers)[number], string[]>();
        for (const head of page) {
          if (!head.deleted) {
            const ids = idsByHandler.get(head.handler) ?? [];
            ids.push(head.id);
            idsByHandler.set(head.handler, ids);
          }
        }
        const payloadsByHandler = new Map<
          (typeof deps.registry.handlers)[number],
          Map<string, unknown>
        >();
        await Promise.all(
          [...idsByHandler.entries()].map(async ([handler, ids]) => {
            payloadsByHandler.set(handler, await handler.reader.loadPayloads(stores, ids));
          }),
        );

        const changes: PullPage["changes"] = page.map((head) => ({
          type: head.handler.type,
          id: head.id,
          version: head.version,
          deleted: head.deleted,
          updatedAt: head.updatedAt,
          payload: head.deleted
            ? null
            : (payloadsByHandler.get(head.handler)?.get(head.id) ?? null),
        }));

        const lastChange = page.at(-1);
        const nextCursor = lastChange ? lastChange.version : cursor;

        const totalEstimate = emptyTotalEstimate();
        await Promise.all(
          deps.registry.handlers.map(async (handler) => {
            const count = await handler.reader.countAfter(stores, userId, cursor);
            totalEstimate[handler.type] += count;
            totalEstimate.total += count;
          }),
        );

        return { changes, nextCursor, hasMore, totalEstimate };
      });
    },
  };
}
