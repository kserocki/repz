import { z } from "@hono/zod-openapi";
import { UserSchema } from "../users/users.schemas.ts";

const EMAIL_MAX_LENGTH = 254;
const REGISTER_PASSWORD_MIN_LENGTH = 8;
const REGISTER_PASSWORD_MAX_LENGTH = 128;
const DISPLAY_NAME_MAX_LENGTH = 50;
// A refresh token is 43 base64url characters; 256 leaves generous room without being unbounded.
const REFRESH_TOKEN_MAX_LENGTH = 256;

/** Trimmed and lowercased before validation: the wire value and the DB lookup always agree. */
function emailField(maxLength?: number) {
  const format = maxLength ? z.email().max(maxLength) : z.email();
  return z.string().trim().toLowerCase().pipe(format);
}

export const RegisterRequestSchema = z
  .object({
    email: emailField(EMAIL_MAX_LENGTH),
    password: z.string().min(REGISTER_PASSWORD_MIN_LENGTH).max(REGISTER_PASSWORD_MAX_LENGTH),
    displayName: z.string().trim().min(1).max(DISPLAY_NAME_MAX_LENGTH),
  })
  .openapi("RegisterRequest", {
    example: {
      email: "alex@example.com",
      password: "correct horse battery staple",
      displayName: "Alex",
    },
  });

export const LoginRequestSchema = z
  .object({
    email: emailField(),
    // Never trimmed (the server just compares it against the stored hash). Capped at the same
    // length as registration: a longer value can never match a registered password anyway, and
    // the cap keeps argon2 from hashing an unbounded input.
    password: z.string().min(1).max(REGISTER_PASSWORD_MAX_LENGTH),
  })
  .openapi("LoginRequest", {
    example: { email: "alex@example.com", password: "correct horse battery staple" },
  });

export const RefreshRequestSchema = z
  .object({ refreshToken: z.string().min(1).max(REFRESH_TOKEN_MAX_LENGTH) })
  .openapi("RefreshRequest", { example: { refreshToken: "3q2+7w==" } });

export const LogoutRequestSchema = z
  .object({ refreshToken: z.string().min(1).max(REFRESH_TOKEN_MAX_LENGTH) })
  .openapi("LogoutRequest", { example: { refreshToken: "3q2+7w==" } });

export const AuthTokensSchema = z
  .object({
    accessToken: z.string(),
    refreshToken: z.string(),
    /** Access token lifetime in seconds (OAuth 2 naming), so the client can refresh ahead of time. */
    expiresIn: z.number().int().positive(),
  })
  .openapi("AuthTokens", {
    example: { accessToken: "eyJhbGciOiJIUzI1NiJ9...", refreshToken: "3q2+7w==", expiresIn: 900 },
  });

export const AuthSessionSchema = AuthTokensSchema.extend({ user: UserSchema }).openapi(
  "AuthSession",
  {
    example: {
      accessToken: "eyJhbGciOiJIUzI1NiJ9...",
      refreshToken: "3q2+7w==",
      expiresIn: 900,
      user: {
        id: "b6e6b6a0-1111-4a22-8a33-000000000001",
        email: "alex@example.com",
        displayName: "Alex",
        unitSystem: "kg",
      },
    },
  },
);
