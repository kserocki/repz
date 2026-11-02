import { describe, expect, it } from "vitest";
import { deriveMuscleGroups, WGER_MUSCLE_TO_GROUP } from "./muscle-mapping.ts";

describe("deriveMuscleGroups", () => {
  it.each(Object.entries(WGER_MUSCLE_TO_GROUP))(
    "given wger muscle id %s, when it is the only primary, then it maps to %s",
    (id, group) => {
      const result = deriveMuscleGroups([Number(id)], []);

      expect(result).toEqual({ ok: true, primaryGroups: [group], secondaryGroups: [] });
    },
  );

  it("given an unknown primary muscle id, when derived, then it fails with that id", () => {
    const result = deriveMuscleGroups([999], []);

    expect(result).toEqual({ ok: false, unknownMuscleId: 999 });
  });

  it("given an unknown secondary muscle id, when derived, then it fails with that id", () => {
    const result = deriveMuscleGroups([4], [999]);

    expect(result).toEqual({ ok: false, unknownMuscleId: 999 });
  });

  it("given two wger primaries mapping to the same group, when derived, then the group appears once", () => {
    // Gastrocnemius (7) and Soleus (15) both map to "calves".
    const result = deriveMuscleGroups([7, 15], []);

    expect(result).toEqual({ ok: true, primaryGroups: ["calves"], secondaryGroups: [] });
  });

  it("given two wger primaries mapping to different groups, when derived, then both groups are reported", () => {
    // Pectoralis major (4, chest) and Triceps brachii (5, triceps).
    const result = deriveMuscleGroups([4, 5], []);

    expect(result).toEqual({ ok: true, primaryGroups: ["chest", "triceps"], secondaryGroups: [] });
  });

  it("given secondary ids that map to the primary's own group, when derived, then the secondary group still appears (de-duplication happens in build-snapshot)", () => {
    const result = deriveMuscleGroups([4], [3]);

    expect(result).toEqual({ ok: true, primaryGroups: ["chest"], secondaryGroups: ["chest"] });
  });
});
