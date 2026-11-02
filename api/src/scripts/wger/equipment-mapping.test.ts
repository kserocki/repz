import { describe, expect, it } from "vitest";
import { deriveEquipment } from "./equipment-mapping.ts";

describe("deriveEquipment", () => {
  it("given no equipment ids, when derived, then it fails", () => {
    expect(deriveEquipment([])).toEqual({ ok: false });
  });

  it.each([
    [[1], "barbell"],
    [[2], "barbell"], // SZ-Bar
    [[3], "dumbbell"],
    [[10], "kettlebell"],
    [[12], "cable"],
    [[11], "band"],
    [[6], "bodyweight"], // Pull-up bar
    [[7], "bodyweight"], // none (bodyweight exercise)
  ] as const)("given wger equipment ids %j, when derived, then it is %s", (ids, expected) => {
    expect(deriveEquipment(ids)).toEqual({ ok: true, equipment: expected });
  });

  it.each([
    [[1, 3], "barbell"],
    [[3, 10], "dumbbell"],
    [[10, 12], "kettlebell"],
    [[12, 11], "cable"],
    [[11, 7], "band"],
  ] as const)(
    "given main items %j together, when derived, then precedence picks %s",
    (ids, expected) => {
      expect(deriveEquipment(ids)).toEqual({ ok: true, equipment: expected });
    },
  );

  it("given Bench, Incline bench and Gym mat with no main item, when derived, then it is bodyweight", () => {
    expect(deriveEquipment([8, 9, 4])).toEqual({ ok: true, equipment: "bodyweight" });
  });

  it("given Swiss Ball alone, when derived, then it is other", () => {
    expect(deriveEquipment([5])).toEqual({ ok: true, equipment: "other" });
  });

  it("given Bench alongside a main item, when derived, then the accessory is ignored", () => {
    expect(deriveEquipment([1, 8])).toEqual({ ok: true, equipment: "barbell" });
  });

  it("given Swiss Ball alongside a main item, when derived, then the accessory is ignored", () => {
    expect(deriveEquipment([3, 5])).toEqual({ ok: true, equipment: "dumbbell" });
  });

  it("given only unrecognised equipment ids, when derived, then it fails", () => {
    expect(deriveEquipment([999])).toEqual({ ok: false });
  });
});
