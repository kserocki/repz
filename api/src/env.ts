import { z } from "zod";

export const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().int().min(1).max(65535).default(3000),
  DATABASE_URL: z.url({ protocol: /^postgres(ql)?$/ }),
  LOG_LEVEL: z.enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"]).default("info"),
  /** >= 32 chars (256+ bits) so it is usable directly as an HS256 key. */
  JWT_SECRET: z.string().min(32),
  ACCESS_TOKEN_TTL_SECONDS: z.coerce.number().int().positive().default(900),
  REFRESH_TOKEN_TTL_SECONDS: z.coerce.number().int().positive().default(2_592_000),
  /** `0` disables the grace window, i.e. strict ADR-0011 reuse detection. */
  REFRESH_TOKEN_REUSE_GRACE_SECONDS: z.coerce.number().int().min(0).default(30),
});

export type Env = z.infer<typeof envSchema>;

/**
 * The subset `pnpm db:migrate` needs. CI's migration step never has `JWT_SECRET`, and a
 * schema migration has no reason to require it.
 */
export const migrationEnvSchema = envSchema.pick({
  NODE_ENV: true,
  DATABASE_URL: true,
  LOG_LEVEL: true,
});

export type MigrationEnv = z.infer<typeof migrationEnvSchema>;

export function loadEnv(source: Record<string, string | undefined>): Env;
export function loadEnv<Schema extends z.ZodTypeAny>(
  source: Record<string, string | undefined>,
  schema: Schema,
): z.infer<Schema>;
/**
 * Parses and validates the process environment against `schema` (the full `envSchema` by
 * default). Throws one Error listing every invalid variable by name, never the value that was
 * supplied for it.
 */
export function loadEnv(
  source: Record<string, string | undefined>,
  schema: z.ZodTypeAny = envSchema,
): unknown {
  const result = schema.safeParse(source);
  if (!result.success) {
    const issues = result.error.issues
      .map((issue) => `${issue.path.join(".") || "(root)"}: ${issue.message}`)
      .join("; ");
    throw new Error(`Invalid environment variables: ${issues}`);
  }
  return result.data;
}
