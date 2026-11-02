import { describe, expect, it } from "vitest";
import {
  collectAuthors,
  licenseUrl,
  mostRestrictiveLicense,
  normaliseLicense,
  resolveLicense,
  sourceUrl,
} from "./attribution.ts";

describe("normaliseLicense", () => {
  it.each([
    ["CC-BY-SA 3", "CC-BY-SA 3.0"],
    ["CC-BY-SA 4", "CC-BY-SA 4.0"],
    ["CC-BY 4", "CC-BY 4.0"],
    ["CC0", "CC0 1.0"],
  ])("given %s, when normalised, then it is %s", (input, expected) => {
    expect(normaliseLicense(input)).toBe(expected);
  });

  it.each(["ODbL", "MIT", ""])("given %s, when normalised, then it is null", (input) => {
    expect(normaliseLicense(input)).toBeNull();
  });
});

describe("mostRestrictiveLicense", () => {
  it("given CC-BY-SA and CC0, when compared, then CC-BY-SA wins", () => {
    expect(mostRestrictiveLicense("CC-BY-SA 4.0", "CC0 1.0")).toBe("CC-BY-SA 4.0");
    expect(mostRestrictiveLicense("CC0 1.0", "CC-BY-SA 4.0")).toBe("CC-BY-SA 4.0");
  });

  it("given CC-BY-SA and CC-BY, when compared, then CC-BY-SA wins", () => {
    expect(mostRestrictiveLicense("CC-BY-SA 3.0", "CC-BY 4.0")).toBe("CC-BY-SA 3.0");
  });

  it("given CC-BY and CC0, when compared, then CC-BY wins", () => {
    expect(mostRestrictiveLicense("CC-BY 4.0", "CC0 1.0")).toBe("CC-BY 4.0");
  });

  it("given the same family at two versions, when compared, then the higher version wins regardless of argument order", () => {
    expect(mostRestrictiveLicense("CC-BY-SA 3.0", "CC-BY-SA 4.0")).toBe("CC-BY-SA 4.0");
    expect(mostRestrictiveLicense("CC-BY-SA 4.0", "CC-BY-SA 3.0")).toBe("CC-BY-SA 4.0");
  });

  it("given the same family and version, when compared, then either argument is returned", () => {
    expect(mostRestrictiveLicense("CC-BY-SA 4.0", "CC-BY-SA 4.0")).toBe("CC-BY-SA 4.0");
  });
});

describe("resolveLicense", () => {
  it("given a CC-BY-SA 3 exercise with a CC-BY-SA 4 EN translation (translation license id 2), when resolved, then the higher version wins (wger 2557)", () => {
    expect(resolveLicense("CC-BY-SA 3", 2)).toEqual({ ok: true, license: "CC-BY-SA 4.0" });
  });

  it("given a CC0 exercise and a CC0 EN translation (license id 3), when resolved, then it is CC0 1.0", () => {
    expect(resolveLicense("CC0", 3)).toEqual({ ok: true, license: "CC0 1.0" });
  });

  it("given an ODbL exercise-level license (id 5 on the translation), when resolved, then it fails", () => {
    expect(resolveLicense("CC-BY-SA 4", 5)).toEqual({ ok: false });
  });

  it("given an unknown translation license id, when resolved, then it fails", () => {
    expect(resolveLicense("CC-BY-SA 4", 999)).toEqual({ ok: false });
  });

  it("given an unrecognised exercise-level short name, when resolved, then it fails", () => {
    expect(resolveLicense("Unknown License", 2)).toEqual({ ok: false });
  });
});

describe("collectAuthors", () => {
  it("given three distinct non-empty authors, when collected, then they are joined in order", () => {
    expect(collectAuthors("a", "b", ["c"])).toBe("a, b, c");
  });

  it("given duplicates across the three sources, when collected, then each author appears once", () => {
    expect(collectAuthors("sistab2", "sistab2", ["sistab2"])).toBe("sistab2");
  });

  it("given null exercise and translation authors with an empty history, when collected, then it falls back to wger.de", () => {
    expect(collectAuthors(null, null, [])).toBe("wger.de");
  });

  it("given blank-string authors, when collected, then they are dropped like empty values", () => {
    expect(collectAuthors("  ", null, ["", "real-author"])).toBe("real-author");
  });

  it("given only an author_history entry, when collected, then it is used", () => {
    expect(collectAuthors(null, null, ["deusinvictus"])).toBe("deusinvictus");
  });
});

describe("sourceUrl", () => {
  it("given a wger id, when built, then it is the public exercise page", () => {
    expect(sourceUrl(73)).toBe("https://wger.de/exercise/73/view-base");
  });
});

describe("licenseUrl", () => {
  it.each([
    ["CC-BY-SA 3.0", "https://creativecommons.org/licenses/by-sa/3.0/"],
    ["CC-BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"],
    ["CC-BY 4.0", "https://creativecommons.org/licenses/by/4.0/"],
    ["CC0 1.0", "https://creativecommons.org/publicdomain/zero/1.0/"],
  ])("given %s, when linked, then it points at the legal code for that license", (license, url) => {
    expect(licenseUrl(license)).toBe(url);
  });
});
