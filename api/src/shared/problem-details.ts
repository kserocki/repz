import { z } from "@hono/zod-openapi";
import type { Context } from "hono";
import type { ContentfulStatusCode } from "hono/utils/http-status";
import type { AppEnv } from "./app-env.ts";

export const ProblemDetailsSchema = z
  .object({
    type: z.string(),
    title: z.string(),
    status: z.number().int(),
    detail: z.string().optional(),
    instance: z.string().optional(),
    requestId: z.string(),
    errors: z.array(z.object({ path: z.string(), message: z.string() })).optional(),
  })
  .openapi("ProblemDetails", {
    example: {
      type: "about:blank",
      title: "Not Found",
      status: 404,
      instance: "/unknown",
      requestId: "b6e6b6a0-1111-4a22-8a33-000000000000",
    },
  });

export type ProblemDetails = {
  type: string;
  title: string;
  status: ContentfulStatusCode;
  detail?: string;
  instance?: string;
  requestId: string;
  errors?: Array<{ path: string; message: string }>;
};

/** Writes a Problem Details response (RFC 9457), stamping the current request id. */
export function problemResponse(
  c: Context<AppEnv>,
  problem: Omit<ProblemDetails, "requestId">,
): Response {
  const body: ProblemDetails = { ...problem, requestId: c.var.requestId };
  return c.body(JSON.stringify(body), body.status, {
    "content-type": "application/problem+json",
  });
}
