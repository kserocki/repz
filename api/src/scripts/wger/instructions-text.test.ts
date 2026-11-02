import { describe, expect, it } from "vitest";
import { htmlToPlainText } from "./instructions-text.ts";

describe("htmlToPlainText", () => {
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

  it("given strong and em tags, when converted, then they are unwrapped but the text is kept", () => {
    const result = htmlToPlainText(
      "<p>Use a <strong>spotter</strong> and breathe <em>slowly</em>.</p>",
    );

    expect(result).toEqual({ ok: true, text: "Use a spotter and breathe slowly." });
  });

  it("given named and numeric entities, when converted, then they are decoded", () => {
    const result = htmlToPlainText(
      "<p>Rock &amp; roll &#8212; it&#39;s &lt;fun&gt;&nbsp;here.</p>",
    );

    expect(result).toEqual({ ok: true, text: "Rock & roll — it's <fun> here." });
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

  it("given an unknown tag, when converted, then it fails naming that tag", () => {
    expect(htmlToPlainText("<p>Text</p><br>")).toEqual({ ok: false, unknownTag: "br" });
  });

  it("given an unknown tag with attributes, when converted, then it still fails naming the tag", () => {
    expect(htmlToPlainText('<div class="x">Text</div>')).toEqual({
      ok: false,
      unknownTag: "div",
    });
  });
});
