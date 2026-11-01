import { DrizzleQueryError } from "drizzle-orm";
import pino from "pino";
import type { Env } from "../env.ts";

const REDACT_PATHS = [
  "req.headers.authorization",
  "req.headers.cookie",
  "*.password",
  "*.token",
  "*.accessToken",
  "*.refreshToken",
];

/** Keeps the stack's frames (no params in those) but replaces the header line(s) that repeat the message. */
function withoutParamsInStack(
  stack: string | undefined,
  sanitizedHeader: string,
): string | undefined {
  if (!stack) {
    return stack;
  }
  const lines = stack.split("\n");
  const firstFrameIndex = lines.findIndex((line) => line.trimStart().startsWith("at "));
  const frames = firstFrameIndex >= 0 ? lines.slice(firstFrameIndex) : [];
  return [sanitizedHeader, ...frames].join("\n");
}

/**
 * `DrizzleQueryError`'s own `message` and `stack` embed the query's bound parameters
 * (`drizzle-orm`'s `errors.js`: `` `Failed query: ${query}\nparams: ${params}` ``), which can be
 * workout names, notes, weights or dates — exactly the payload data that must never reach the
 * logs. Builds a stand-in error that keeps the query text, the error's own name, and the
 * underlying driver error's SQLSTATE `code` and `constraint_name`, but drops `params` and the
 * params portion of the message and stack entirely.
 */
function sanitizeDrizzleQueryError(error: DrizzleQueryError): Error {
  const message = `Failed query: ${error.query}`;
  const safeError = new Error(message) as Error & Record<string, unknown>;
  safeError.name = error.constructor.name;
  safeError.stack = withoutParamsInStack(error.stack, `${safeError.name}: ${message}`);

  const cause = error.cause;
  if (cause && typeof cause === "object") {
    if ("code" in cause) {
      safeError.code = (cause as Record<string, unknown>).code;
    }
    if ("constraint_name" in cause) {
      safeError.constraintName = (cause as Record<string, unknown>).constraint_name;
    }
  }
  return safeError;
}

/**
 * Registered on the `err` key, so every `logger.error({ err }, ...)` call anywhere (including
 * child loggers, which inherit serializers from the root) is sanitized the same way, not just one
 * call site.
 */
function errSerializer(error: unknown): unknown {
  const safeError = error instanceof DrizzleQueryError ? sanitizeDrizzleQueryError(error) : error;
  return pino.stdSerializers.err(safeError as Error);
}

/** Creates the process-wide pino logger. JSON to stdout; `pnpm dev` pipes it through pino-pretty. */
export function createLogger(
  env: Pick<Env, "LOG_LEVEL">,
  destination?: pino.DestinationStream,
): pino.Logger {
  return pino(
    {
      level: env.LOG_LEVEL,
      redact: { paths: REDACT_PATHS, censor: "[REDACTED]" },
      serializers: { err: errSerializer },
    },
    destination,
  );
}
