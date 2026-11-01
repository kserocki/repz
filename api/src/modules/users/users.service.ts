import { InvalidAccessTokenError } from "../auth/auth.errors.ts";
import type { ProfileChanges, UserRecord, UsersRepository } from "./users.repository.ts";

export type UsersService = {
  getProfile(userId: string): Promise<UserRecord>;
  updateProfile(userId: string, changes: ProfileChanges): Promise<UserRecord>;
};

/**
 * Behind `requireAuth`: `userId` always comes from a verified access token, so a missing user
 * means the account was deleted after the token was issued.
 */
export function createUsersService(users: UsersRepository): UsersService {
  return {
    async getProfile(userId) {
      const user = await users.findById(userId);
      if (!user) {
        throw new InvalidAccessTokenError("invalid");
      }
      return user;
    },

    async updateProfile(userId, changes) {
      const user = await users.updateProfile(userId, changes);
      if (!user) {
        throw new InvalidAccessTokenError("invalid");
      }
      return user;
    },
  };
}
