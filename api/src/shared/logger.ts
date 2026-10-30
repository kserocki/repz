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

/** Creates the process-wide pino logger. JSON to stdout; `pnpm dev` pipes it through pino-pretty. */
export function createLogger(
  env: Pick<Env, "LOG_LEVEL">,
  destination?: pino.DestinationStream,
): pino.Logger {
  return pino(
    {
      level: env.LOG_LEVEL,
      redact: { paths: REDACT_PATHS, censor: "[REDACTED]" },
    },
    destination,
  );
}
