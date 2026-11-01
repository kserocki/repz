import { z } from "@hono/zod-openapi";
import { UNIT_SYSTEMS } from "../../db/schema/vocabularies.ts";
import type { UserRecord } from "./users.repository.ts";

const DISPLAY_NAME_MAX_LENGTH = 50;

export const UnitSystemSchema = z.enum(UNIT_SYSTEMS);

export const UserSchema = z
  .object({
    id: z.uuid(),
    email: z.email(),
    displayName: z.string(),
    unitSystem: UnitSystemSchema,
  })
  .openapi("User", {
    example: {
      id: "b6e6b6a0-1111-4a22-8a33-000000000001",
      email: "alex@example.com",
      displayName: "Alex",
      unitSystem: "kg",
    },
  });

export type User = z.infer<typeof UserSchema>;

/** The wire shape never carries `passwordHash`; routes map every `UserRecord` through this. */
export function toUserResponse(user: UserRecord): User {
  return {
    id: user.id,
    email: user.email,
    displayName: user.displayName,
    unitSystem: user.unitSystem,
  };
}

const displayNameField = z.string().trim().min(1).max(DISPLAY_NAME_MAX_LENGTH);

export const UpdateProfileRequestSchema = z
  .strictObject({
    displayName: displayNameField.optional(),
    unitSystem: UnitSystemSchema.optional(),
  })
  .refine((body) => body.displayName !== undefined || body.unitSystem !== undefined, {
    message: "At least one of displayName or unitSystem is required",
  })
  .openapi("UpdateProfileRequest", { example: { unitSystem: "lb" } });
