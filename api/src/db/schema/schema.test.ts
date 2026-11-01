import { getTableConfig, PgDialect, type PgTable } from "drizzle-orm/pg-core";
import { describe, expect, it } from "vitest";
import {
  exercises,
  refreshTokens,
  templateExercises,
  users,
  workoutExercises,
  workoutSets,
  workouts,
  workoutTemplates,
} from "./index.ts";
import { EQUIPMENT, SET_TYPES, UNIT_SYSTEMS } from "./vocabularies.ts";

const dialect = new PgDialect();
const TIMESTAMPTZ_3 = "timestamp (3) with time zone";

function column(table: PgTable, name: string) {
  const found = getTableConfig(table).columns.find((c) => c.name === name);
  if (!found) {
    throw new Error(`Column "${name}" not found on "${getTableConfig(table).name}"`);
  }
  return found;
}

function check(table: PgTable, name: string) {
  const found = getTableConfig(table).checks.find((c) => c.name === name);
  if (!found) {
    throw new Error(`Check "${name}" not found on "${getTableConfig(table).name}"`);
  }
  return found;
}

function foreignKeyOn(table: PgTable, columnName: string) {
  return getTableConfig(table).foreignKeys.find((fk) =>
    fk.reference().columns.some((c) => c.name === columnName),
  );
}

function index(table: PgTable, name: string) {
  return getTableConfig(table).indexes.find((i) => i.config.name === name);
}

function tableName(table: PgTable): string {
  return getTableConfig(table).name;
}

/** The literal values of a `col in ('a', 'b', ...)` check, in the order they were written. */
function inListValues(checkSql: ReturnType<typeof check>): string[] {
  const { sql } = dialect.sqlToQuery(checkSql.value);
  const match = sql.match(/in \(([^)]*)\)/i);
  if (!match?.[1]) {
    throw new Error(`No "in (...)" list found in: ${sql}`);
  }
  return match[1].split(",").map((value) => value.trim().slice(1, -1));
}

describe("syncable roots", () => {
  const roots: [string, PgTable][] = [
    ["exercises", exercises],
    ["workoutTemplates", workoutTemplates],
    ["workouts", workouts],
  ];

  it.each(roots)("given %s, then it has version, updatedAt and deletedAt", (_name, table) => {
    const version = column(table, "version");
    const updatedAt = column(table, "updated_at");
    const deletedAt = column(table, "deleted_at");

    expect(version.notNull).toBe(true);
    expect(version.columnType).toBe("PgBigInt53");
    expect(version.hasDefault).toBe(true);
    expect(
      dialect.sqlToQuery(version.default as Parameters<typeof dialect.sqlToQuery>[0]).sql,
    ).toContain("nextval('sync_version_seq')");

    expect(updatedAt.notNull).toBe(true);
    expect(updatedAt.getSQLType()).toBe(TIMESTAMPTZ_3);

    expect(deletedAt.notNull).toBe(false);
    expect(deletedAt.getSQLType()).toBe(TIMESTAMPTZ_3);
  });

  const userScopedRoots: [string, PgTable, string][] = [
    ["workoutTemplates", workoutTemplates, "user_id"],
    ["workouts", workouts, "user_id"],
  ];

  it.each(userScopedRoots)(
    "given %s, then %s is not null with a cascading FK to users",
    (_name, table, columnName) => {
      expect(column(table, columnName).notNull).toBe(true);

      const fk = foreignKeyOn(table, columnName);
      expect(fk?.onDelete).toBe("cascade");
      expect(fk && tableName(fk.reference().foreignTable)).toBe(tableName(users));
      expect(fk?.reference().foreignColumns.map((c) => c.name)).toEqual(["id"]);
    },
  );

  it("given exercises, then owner_id is nullable with a cascading FK to users", () => {
    expect(column(exercises, "owner_id").notNull).toBe(false);

    const fk = foreignKeyOn(exercises, "owner_id");
    expect(fk?.onDelete).toBe("cascade");
    expect(fk && tableName(fk.reference().foreignTable)).toBe(tableName(users));
    expect(fk?.reference().foreignColumns.map((c) => c.name)).toEqual(["id"]);
  });

  const rootScopeIndexes: [string, PgTable, string][] = [
    ["exercises", exercises, "exercises_owner_id_version_idx"],
    ["workoutTemplates", workoutTemplates, "workout_templates_user_id_version_idx"],
    ["workouts", workouts, "workouts_user_id_version_idx"],
  ];

  it.each(rootScopeIndexes)(
    "given %s, then it has a (scope, version) index",
    (_name, table, indexName) => {
      const idx = index(table, indexName);

      expect(idx).toBeDefined();
      expect(idx?.config.columns.at(-1)).toMatchObject({ name: "version" });
    },
  );
});

describe("child tables", () => {
  const childrenWithCascadingParentFk: [string, PgTable, string, PgTable][] = [
    ["refreshTokens", refreshTokens, "user_id", users],
    ["templateExercises", templateExercises, "template_id", workoutTemplates],
    ["workoutExercises", workoutExercises, "workout_id", workouts],
    ["workoutSets", workoutSets, "workout_exercise_id", workoutExercises],
  ];

  it.each(childrenWithCascadingParentFk)(
    "given %s, then its parent FK cascades on delete",
    (_name, table, columnName, parentTable) => {
      const fk = foreignKeyOn(table, columnName);

      expect(fk).toBeDefined();
      expect(fk?.onDelete).toBe("cascade");
      expect(fk && tableName(fk.reference().foreignTable)).toBe(tableName(parentTable));
    },
  );

  const crossAggregateColumns: [string, PgTable, string][] = [
    ["workouts", workouts, "template_id"],
    ["workoutExercises", workoutExercises, "exercise_id"],
    ["templateExercises", templateExercises, "exercise_id"],
  ];

  it.each(crossAggregateColumns)(
    "given %s, then %s has no foreign key (cross-aggregate reference)",
    (_name, table, columnName) => {
      expect(foreignKeyOn(table, columnName)).toBeUndefined();
    },
  );
});

describe("refresh token rotation", () => {
  it("given refreshTokens, then replaced_by is a nullable uuid", () => {
    const replacedBy = column(refreshTokens, "replaced_by");

    expect(replacedBy.notNull).toBe(false);
    expect(replacedBy.columnType).toBe("PgUUID");
  });

  it("given refreshTokens, then replaced_by has a self-referencing FK that sets null on delete", () => {
    const fk = foreignKeyOn(refreshTokens, "replaced_by");

    expect(fk).toBeDefined();
    expect(fk?.onDelete).toBe("set null");
    expect(fk && tableName(fk.reference().foreignTable)).toBe(tableName(refreshTokens));
    expect(fk?.reference().foreignColumns.map((c) => c.name)).toEqual(["id"]);
  });

  it("given refreshTokens, then the replaced_by check constraint exists", () => {
    expect(() => check(refreshTokens, "refresh_tokens_replaced_by_check")).not.toThrow();
  });
});

describe("vocabularies", () => {
  it("given the exercises equipment check, when read, then it matches EQUIPMENT", () => {
    expect(inListValues(check(exercises, "exercises_equipment_check"))).toEqual([...EQUIPMENT]);
  });

  it("given the workout_sets type check, when read, then it matches SET_TYPES", () => {
    expect(inListValues(check(workoutSets, "workout_sets_type_check"))).toEqual([...SET_TYPES]);
  });

  it("given the users unit_system check, when read, then it matches UNIT_SYSTEMS", () => {
    expect(inListValues(check(users, "users_unit_system_check"))).toEqual([...UNIT_SYSTEMS]);
  });
});

describe("timestamps", () => {
  const tables: [string, PgTable][] = [
    ["users", users],
    ["refreshTokens", refreshTokens],
    ["exercises", exercises],
    ["workoutTemplates", workoutTemplates],
    ["workouts", workouts],
    ["workoutSets", workoutSets],
  ];

  it.each(tables)("given %s, then every timestamp column is timestamptz(3)", (_name, table) => {
    const timestampColumns = getTableConfig(table).columns.filter(
      (c) => c.columnType === "PgTimestamp",
    );

    expect(timestampColumns.length).toBeGreaterThan(0);
    for (const timestampColumn of timestampColumns) {
      expect(timestampColumn.getSQLType()).toBe(TIMESTAMPTZ_3);
    }
  });
});
