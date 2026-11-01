import type pino from "pino";
import type { Clock } from "../../shared/clock.ts";
import type { UserRecord, UsersRepository } from "../users/users.repository.ts";
import type { AccessTokens } from "./access-tokens.ts";
import {
  InvalidCredentialsError,
  InvalidRefreshTokenError,
  RefreshTokenReusedError,
} from "./auth.errors.ts";
import type {
  AuthTransactions,
  RefreshTokenRecord,
  RefreshTokensRepository,
} from "./auth.repository.ts";
import type { PasswordHasher } from "./password-hasher.ts";
import { decideRefresh } from "./refresh-rotation.ts";
import { generateRefreshToken, hashRefreshToken } from "./refresh-token.ts";

export type AuthSettings = {
  refreshTokenTtlSeconds: number;
  refreshTokenReuseGraceSeconds: number;
};

export type AuthSession = {
  user: UserRecord;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

export type AuthTokens = Omit<AuthSession, "user">;

export type AuthService = {
  register(
    input: { email: string; password: string; displayName: string },
    logger: pino.Logger,
  ): Promise<AuthSession>;
  login(input: { email: string; password: string }, logger: pino.Logger): Promise<AuthSession>;
  refresh(refreshToken: string, logger: pino.Logger): Promise<AuthTokens>;
  logout(refreshToken: string): Promise<void>;
};

/**
 * Used only when an email is unknown, so one argon2 verify always runs either way and login
 * timing doesn't reveal whether the account exists.
 */
const DUMMY_PASSWORD = "repz-dummy-password-for-constant-time-login-failures";

type RefreshOutcome =
  | {
      kind: "rotate" | "replay-within-grace";
      userId: string;
      familyId: string;
      refreshToken: string;
    }
  | { kind: "reuse"; userId: string; familyId: string }
  | { kind: "invalid" | "expired" };

async function issueRefreshToken(
  refreshTokens: RefreshTokensRepository,
  family: { userId: string; familyId: string },
  expiresAt: Date,
): Promise<{ record: RefreshTokenRecord; rawToken: string }> {
  const rawToken = generateRefreshToken();
  const record = await refreshTokens.insert({
    userId: family.userId,
    familyId: family.familyId,
    tokenHash: hashRefreshToken(rawToken),
    expiresAt,
  });
  return { record, rawToken };
}

export function createAuthService(deps: {
  transactions: AuthTransactions;
  users: UsersRepository;
  passwordHasher: PasswordHasher;
  accessTokens: AccessTokens;
  clock: Clock;
  generateId: () => string;
  settings: AuthSettings;
}): AuthService {
  // Started once, at construction time (app startup), and reused by every login.
  const dummyPasswordHash = deps.passwordHasher.hash(DUMMY_PASSWORD);
  // Nothing awaits this promise until the first login; without a handler here, a rejection
  // (e.g. the hasher failing) would surface as an unhandled rejection before that happens.
  // `login()` still awaits the same promise and sees the real rejection there.
  dummyPasswordHash.catch(() => {});

  function refreshTokenExpiry(): Date {
    return new Date(deps.clock.now().getTime() + deps.settings.refreshTokenTtlSeconds * 1000);
  }

  async function startSession(user: UserRecord): Promise<AuthSession> {
    const familyId = deps.generateId();
    const { rawToken } = await deps.transactions.run(({ refreshTokens }) =>
      issueRefreshToken(refreshTokens, { userId: user.id, familyId }, refreshTokenExpiry()),
    );
    const { accessToken, expiresIn } = await deps.accessTokens.issue(user.id);
    return { user, accessToken, refreshToken: rawToken, expiresIn };
  }

  return {
    async register({ email, password, displayName }, logger) {
      // Defense in depth: the route's Zod schema already does this, but the stored value must
      // be normalized regardless of caller.
      const normalizedEmail = email.trim().toLowerCase();
      const normalizedDisplayName = displayName.trim();
      const passwordHash = await deps.passwordHasher.hash(password);

      const { user, rawToken } = await deps.transactions.run(async ({ users, refreshTokens }) => {
        const insertedUser = await users.insert({
          email: normalizedEmail,
          passwordHash,
          displayName: normalizedDisplayName,
        });
        const familyId = deps.generateId();
        const { rawToken: token } = await issueRefreshToken(
          refreshTokens,
          { userId: insertedUser.id, familyId },
          refreshTokenExpiry(),
        );
        return { user: insertedUser, rawToken: token };
      });

      const { accessToken, expiresIn } = await deps.accessTokens.issue(user.id);
      logger.info({ userId: user.id }, "user registered");
      return { user, accessToken, refreshToken: rawToken, expiresIn };
    },

    async login({ email, password }, logger) {
      // Defense in depth, matching register: the route already normalizes this, and the
      // repository's lookup is already case-insensitive, so this changes nothing functionally.
      const normalizedEmail = email.trim().toLowerCase();
      const user = await deps.users.findByEmail(normalizedEmail);
      if (!user) {
        await deps.passwordHasher.verify(await dummyPasswordHash, password);
        throw new InvalidCredentialsError();
      }

      const passwordMatches = await deps.passwordHasher.verify(user.passwordHash, password);
      if (!passwordMatches) {
        throw new InvalidCredentialsError();
      }

      const session = await startSession(user);
      logger.info({ userId: user.id }, "user logged in");
      return session;
    },

    async refresh(refreshToken, logger) {
      const presentedHash = hashRefreshToken(refreshToken);

      const outcome: RefreshOutcome = await deps.transactions.run(async ({ refreshTokens }) => {
        const presented = await refreshTokens.findByHashForUpdate(presentedHash);
        const successor = presented?.replacedBy
          ? await refreshTokens.findByIdForUpdate(presented.replacedBy)
          : undefined;

        const now = deps.clock.now();
        const decision = decideRefresh({
          presented,
          successor,
          now,
          graceSeconds: deps.settings.refreshTokenReuseGraceSeconds,
        });

        if (decision === "invalid" || decision === "expired") {
          return { kind: decision };
        }
        if (!presented) {
          // decideRefresh never returns a token decision without a presented row; a type-safety net.
          throw new Error("decideRefresh returned a token decision without a presented token");
        }

        if (decision === "reuse") {
          await refreshTokens.revokeFamily(presented.familyId, now);
          return { kind: "reuse", userId: presented.userId, familyId: presented.familyId };
        }

        const { record, rawToken } = await issueRefreshToken(
          refreshTokens,
          { userId: presented.userId, familyId: presented.familyId },
          new Date(now.getTime() + deps.settings.refreshTokenTtlSeconds * 1000),
        );
        if (decision === "rotate") {
          await refreshTokens.markRotated(presented.id, now, record.id);
        }
        return {
          kind: decision,
          userId: presented.userId,
          familyId: presented.familyId,
          refreshToken: rawToken,
        };
      });

      switch (outcome.kind) {
        case "invalid":
        case "expired":
          throw new InvalidRefreshTokenError();
        case "reuse":
          throw new RefreshTokenReusedError(outcome.userId, outcome.familyId);
        case "replay-within-grace":
          logger.info(
            { userId: outcome.userId, familyId: outcome.familyId },
            "refresh token replayed within grace window",
          );
          break;
        case "rotate":
          break;
      }

      const { accessToken, expiresIn } = await deps.accessTokens.issue(outcome.userId);
      return { accessToken, refreshToken: outcome.refreshToken, expiresIn };
    },

    async logout(refreshToken) {
      const hash = hashRefreshToken(refreshToken);
      await deps.transactions.run(async ({ refreshTokens }) => {
        const presented = await refreshTokens.findByHashForUpdate(hash);
        if (presented) {
          await refreshTokens.revokeFamily(presented.familyId, deps.clock.now());
        }
      });
    },
  };
}
