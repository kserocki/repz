import { describe, expect, it } from "vitest";
import { htmlToPlainText } from "./instructions-text.ts";

describe("htmlToPlainText happy path", () => {
  it("given a single paragraph, when converted, then it is one line", () => {
    const result = htmlToPlainText("<p>Lay down on a bench.</p>");

    expect(result).toEqual({ ok: true, text: "Lay down on a bench." });
  });

  it("given two paragraphs, when converted, then they are separated by a blank line", () => {
    const result = htmlToPlainText("<p>First.</p>\n<p>Second.</p>");

    expect(result).toEqual({ ok: true, text: "First.\n\nSecond." });
  });

  it("given an ordered list, when converted, then items are numbered", () => {
    const result = htmlToPlainText("<ol>\n<li>Stand up.</li>\n<li>Sit down.</li>\n</ol>");

    expect(result).toEqual({ ok: true, text: "1. Stand up.\n2. Sit down." });
  });

  it("given an unordered list, when converted, then items are bulleted", () => {
    const result = htmlToPlainText("<ul>\n<li>Wide grip.</li>\n<li>Narrow grip.</li>\n</ul>");

    expect(result).toEqual({ ok: true, text: "• Wide grip.\n• Narrow grip." });
  });

  it("given a paragraph followed by a list, when converted, then they are joined with a blank line", () => {
    const result = htmlToPlainText("<p>Intro.</p>\n<ol>\n<li>Step one.</li>\n</ol>");

    expect(result).toEqual({ ok: true, text: "Intro.\n\n1. Step one." });
  });

  it("given strong and em tags inside a paragraph, when converted, then they are unwrapped but the text is kept", () => {
    const result = htmlToPlainText(
      "<p>Use a <strong>spotter</strong> and breathe <em>slowly</em>.</p>",
    );

    expect(result).toEqual({ ok: true, text: "Use a spotter and breathe slowly." });
  });

  it("given strong inside a list item, when converted, then it is unwrapped (wger 1573's real shape)", () => {
    const result = htmlToPlainText(
      "<ol><li><strong>Starting Position</strong>: kneel down.</li></ol>",
    );

    expect(result).toEqual({ ok: true, text: "1. Starting Position: kneel down." });
  });

  it("given named and numeric entities, when converted, then they are decoded", () => {
    const result = htmlToPlainText("<p>Rock &amp; roll &#8212; it&#39;s great&nbsp;here.</p>");

    expect(result).toEqual({ ok: true, text: "Rock & roll — it's great here." });
  });

  it("given a decoded entity that produces a digit-adjacent angle bracket, when converted, then it is kept (not mistaken for markup)", () => {
    const result = htmlToPlainText("<p>Do fewer &lt;10 or more &gt;20 reps.</p>");

    expect(result).toEqual({ ok: true, text: "Do fewer <10 or more >20 reps." });
  });

  it("given excess whitespace, when converted, then it is collapsed to single spaces", () => {
    const result = htmlToPlainText("<p>Too   much\n\n  whitespace   here.</p>");

    expect(result).toEqual({ ok: true, text: "Too much whitespace here." });
  });

  it("given an empty string, when converted, then the result is null", () => {
    expect(htmlToPlainText("")).toEqual({ ok: true, text: null });
  });

  it("given only whitespace paragraphs, when converted, then the result is null", () => {
    expect(htmlToPlainText("<p>   </p>")).toEqual({ ok: true, text: null });
  });
});

describe("htmlToPlainText problems: tags", () => {
  it("given an unknown tag, when converted, then it fails naming that tag", () => {
    const result = htmlToPlainText("<p>Text</p><br>");

    expect(result).toEqual({ ok: false, reason: expect.stringContaining("<br>") });
  });

  it("given a known tag name with an attribute, when converted, then it fails (never silently stripped)", () => {
    const result = htmlToPlainText('<p class="intro">Text</p>');

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain('<p class="intro">');
    }
  });

  it("given an unknown tag with attributes, when converted, then it still fails naming the tag", () => {
    const result = htmlToPlainText('<div class="x">Text</div>');

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain('<div class="x">');
    }
  });

  it("given an upper-case known tag, when converted, then it fails (case matters)", () => {
    const result = htmlToPlainText("<P>Text</P>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("<P>");
    }
  });

  it("given a paragraph nested inside a list item (wger 723/75/537's real shape), when converted, then it fails", () => {
    const result = htmlToPlainText("<ol><li><p>Starting position.</p></li></ol>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("nested <p> inside <li>");
    }
  });

  it("given a list nested inside a list item (wger 723's real shape), when converted, then it fails", () => {
    const result = htmlToPlainText("<ol><li><ul><li>Inner item.</li></ul></li></ol>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("nested <ul> inside <li>");
    }
  });

  it("given a li outside any ol/ul, when converted, then it fails", () => {
    const result = htmlToPlainText("<li>Orphan item.</li>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("<li> outside <ol>/<ul>");
    }
  });

  it("given strong outside a paragraph or list item, when converted, then it fails", () => {
    const result = htmlToPlainText("<strong>Bold.</strong>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("outside a paragraph or list item");
    }
  });

  it("given a mismatched closing tag, when converted, then it fails", () => {
    const result = htmlToPlainText("<p><strong>Text</p></strong>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("mismatched closing tag");
    }
  });

  it("given an unclosed tag, when converted, then it fails", () => {
    const result = htmlToPlainText("<p>Text");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("unclosed tag <p>");
    }
  });
});

describe("htmlToPlainText problems: text placement", () => {
  it("given bare text with no block tag at all, when converted, then it fails", () => {
    const result = htmlToPlainText("Just some text, no tags.");

    expect(result).toEqual({ ok: false, reason: "text outside a block" });
  });

  it("given text between two blocks, when converted, then it fails", () => {
    const result = htmlToPlainText("<p>Intro</p>Extra<ul><li>Item</li></ul>");

    expect(result).toEqual({ ok: false, reason: "text outside a block" });
  });

  it("given text directly inside ul (not wrapped in li), when converted, then it fails", () => {
    const result = htmlToPlainText("<ul>Loose text<li>Item</li></ul>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("text directly inside <ul>");
    }
  });
});

describe("htmlToPlainText problems: entities", () => {
  it("given an out-of-range numeric entity, when converted, then it fails instead of throwing", () => {
    const result = htmlToPlainText("<p>Text &#99999999;</p>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("&#99999999;");
    }
  });

  it("given a numeric entity in the surrogate range, when converted, then it fails instead of throwing", () => {
    const result = htmlToPlainText("<p>Text &#55296;</p>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("&#55296;");
    }
  });

  it("given an unknown named entity, when converted, then it fails rather than leaking raw markup-adjacent text", () => {
    const result = htmlToPlainText("<p>Rock &mdash; roll.</p>");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.reason).toContain("&mdash;");
    }
  });
});
