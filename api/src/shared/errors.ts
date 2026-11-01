import { STATUS_CODES } from "node:http";
import type { Hook } from "@hono/zod-openapi";
import type { Context, ErrorHandler, NotFoundHandler } from "hono";
import { HTTPException } from "hono/http-exception";
import type { ContentfulStatusCode } from "hono/utils/http-status";
import type { AppEnv } from "./app-env.ts";
import { ApplicationError } from "./application-error.ts";
import { problemResponse } from "./problem-details.ts";

function reasonPhrase(status: number): string {
  return STATUS_CODES[status] ?? "Error";
}

/** Generic over `E` so it also works from a router with a wider env, e.g. `AuthenticatedAppEnv`. */
function requestPath<E extends AppEnv>(c: Context<E>): string {
  return new URL(c.req.url).pathname;
}

export const notFoundHandler: NotFoundHandler<AppEnv> = (c) => {
  return problemResponse(c, {
    type: "about:blank",
    title: reasonPhrase(404),
    status: 404,
    instance: requestPath(c),
  });
};

export const errorHandler: ErrorHandler<AppEnv> = (error, c) => {
  if (error instanceof ApplicationError) {
    if (error.log) {
      c.var.logger[error.log.level](error.log.fields, error.log.message);
    }
    return problemResponse(
      c,
      {
        type: error.type,
        title: error.title,
        status: error.status,
        instance: requestPath(c),
      },
      error.headers,
    );
  }

  if (error instanceof HTTPException) {
    const status = error.status;
    if (status >= 500) {
      c.var.logger.error({ err: error }, "HTTP exception");
    }
    return problemResponse(c, {
      type: "about:blank",
      title: reasonPhrase(status),
      status,
      detail: error.message || undefined,
      instance: requestPath(c),
    });
  }

  c.var.logger.error({ err: error }, "Unhandled error");
  const status: ContentfulStatusCode = 500;
  return problemResponse(c, {
    type: "about:blank",
    title: reasonPhrase(status),
    status,
    instance: requestPath(c),
  });
};

/**
 * `OpenAPIHono` default validation hook: on a Zod failure, replies with
 * `urn:repz:problem:validation-error` and one `{ path, message }` entry per
 * issue, path prefixed by the validation target (e.g. "query.limit"). Generic over `E` for the
 * same reason as `problemResponse`, so `createRouter<AuthenticatedAppEnv>()` can use it too.
 */
export function validationErrorHook<E extends AppEnv = AppEnv>(
  ...[result, c]: Parameters<Hook<unknown, E, string, Response | undefined>>
): Response | undefined {
  if (result.success) {
    return undefined;
  }

  const errors = result.error.issues.map((issue) => ({
    path: [result.target, ...issue.path.map(String)].join("."),
    message: issue.message,
  }));

  return problemResponse(c, {
    type: "urn:repz:problem:validation-error",
    title: "Validation Error",
    status: 400,
    instance: requestPath(c),
    errors,
  });
}
