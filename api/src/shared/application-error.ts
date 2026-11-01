/**
 * Base for every typed domain error that `errorHandler` maps to an RFC 9457 Problem Details
 * response. A subclass fixes `type`/`title`/`status`; `headers` and `log` are optional extras
 * (e.g. `WWW-Authenticate` on an invalid access token, a warn log line on refresh reuse). A
 * subclass that needs a dynamic `headers` or `log` value redeclares the field and assigns it
 * in its own constructor (erasable syntax only: no parameter properties).
 */
export abstract class ApplicationError extends Error {
  abstract readonly type: `urn:repz:problem:${string}`;
  abstract readonly title: string;
  abstract readonly status: 400 | 401 | 403 | 404 | 409;
  readonly headers: Record<string, string> = {};
  readonly log?: { level: "info" | "warn"; message: string; fields: Record<string, unknown> };
}
