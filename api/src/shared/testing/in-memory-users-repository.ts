import { EmailTakenError } from "../../modules/auth/auth.errors.ts";
import type { UserRecord, UsersRepository } from "../../modules/users/users.repository.ts";

/** DB-free `UsersRepository` fake: a `Map` keyed by id, with the same case-insensitive email uniqueness. */
export function createInMemoryUsersRepository(
  generateId: () => string = () => crypto.randomUUID(),
): UsersRepository {
  const usersById = new Map<string, UserRecord>();

  function findByEmailSync(email: string): UserRecord | undefined {
    const lowerEmail = email.toLowerCase();
    return [...usersById.values()].find((user) => user.email.toLowerCase() === lowerEmail);
  }

  return {
    async insert(user) {
      if (findByEmailSync(user.email)) {
        throw new EmailTakenError();
      }
      const record: UserRecord = { id: generateId(), unitSystem: "kg", ...user };
      usersById.set(record.id, record);
      return record;
    },

    async findByEmail(email) {
      return findByEmailSync(email);
    },

    async findById(id) {
      return usersById.get(id);
    },

    async updateProfile(id, changes) {
      const existing = usersById.get(id);
      if (!existing) {
        return undefined;
      }
      const updated: UserRecord = { ...existing, ...changes };
      usersById.set(id, updated);
      return updated;
    },
  };
}
